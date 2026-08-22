package com.nimbusnovax.voucher.core;

import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.administracao.model.AgentAddress;
import com.nimbusnovax.administracao.model.AgentContact;
import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.repository.CancellationReasonRepository;
import com.nimbusnovax.common.company.CompanySettingsService;
import com.nimbusnovax.common.notification.mail.EmailSenderService;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
import com.nimbusnovax.voucher.model.ConfigVoucher;
import com.nimbusnovax.voucher.model.Voucher;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import com.nimbusnovax.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Fluxo de status de um voucher - replica o {@code VoucherFlowService} do sistema legado (Novax
 * antigo): confirmar/não confirmar/trocar/cancelar/enviar por e-mail/visualizar em PDF. Cada
 * transição delega a defesa de verdade pra {@link Voucher} ({@code confirm()}/{@code change()}/
 * etc. só têm efeito se {@code canChangeStatus()} permitir) - aqui só resolvemos as dependências
 * (motivo de cancelamento, configuração, envio de e-mail) e checamos a permissão de cada ação.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class VoucherFlowService {

  private final VoucherRepository voucherRepository;
  private final CancellationReasonRepository cancellationReasonRepository;
  private final VoucherService voucherService;
  private final ConfigVoucherService configVoucherService;
  private final CompanySettingsService companySettingsService;
  private final VoucherPdfService pdfService;
  private final EmailSenderService emailSenderService;
  private final CurrentUserProvider currentUserProvider;

  /** Voucher com valor zerado não pode ser confirmado - sem valor não há o que cobrar do cliente,
   *  então a confirmação (que trava o voucher no fluxo de venda) não faz sentido de negócio. */
  public void confirm(UUID id) {
    requireAuthority("VOUCHERS_CHANGE");
    Voucher voucher = getOrThrow(id);

    if (voucher.getTotalPrice() == null || voucher.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível confirmar um voucher com valor zerado.");
    }

    voucher.confirm();
    voucher.setUpdatedById(currentUserId());
  }

  public void notConfirm(UUID id) {
    requireAuthority("VOUCHERS_CHANGE");
    Voucher voucher = getOrThrow(id);
    voucher.notConfirm();
    voucher.setUpdatedById(currentUserId());
  }

  /** Só é permitido trocar (marcar como acessado) um voucher já CONFIRMED - trocar a partir de
   *  qualquer outro status não faz sentido de negócio (o cliente precisa ter confirmado a visita
   *  antes de ser considerado "acessado"). */
  public void change(UUID id) {
    requireAuthority("VOUCHERS_CHANGE");
    Voucher voucher = getOrThrow(id);

    if (voucher.getStatusEnum() != StatusVoucherEnum.CONFIRMED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Somente um voucher Confirmado pode ser trocado (status atual: " + voucher.getStatusEnum() + ").");
    }

    voucher.change();
    voucher.setUpdatedById(currentUserId());

    ConfigVoucher config = configVoucherService.getOrCreate();
    if (Boolean.TRUE.equals(config.getSenderMail())) {
      sendChangeNotification(voucher, config);
    }
  }

  public void cancel(UUID id, UUID cancellationReasonId) {
    requireAuthority("VOUCHERS_CHANGE");
    Voucher voucher = getOrThrow(id);
    CancellationReason reason = cancellationReasonRepository.findById(cancellationReasonId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Motivo de cancelamento não encontrado: " + cancellationReasonId));

    voucher.cancel(reason);
    voucher.setUpdatedById(currentUserId());
  }

  public void sendVoucherEmail(UUID id) {
    requireAuthority("VOUCHERS_CHANGE");
    Voucher voucher = getOrThrow(id);

    // Voucher.canSend() no legado: bloqueia envio só quando CALLED_OFF ou OVERDUE (um voucher
    // ainda em negociação, confirmado ou trocado pode ser enviado normalmente).
    var status = voucher.getStatusEnum();
    if (status == StatusVoucherEnum.CALLED_OFF || status == StatusVoucherEnum.OVERDUE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher com status " + status + " não pode ser enviado.");
    }

    List<String> recipients = voucher.getClient().getContacts().stream()
        .map(AgentContact::getEmail)
        .filter(email -> email != null && !email.isBlank())
        .toList();

    if (recipients.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Não é possível enviar e-mail - o cliente não possui contato cadastrado.");
    }

    String replyTo = voucher.getPromoter().getContacts().stream()
        .map(AgentContact::getEmail)
        .filter(email -> email != null && !email.isBlank())
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Complete o cadastro do promotor - contato inválido."));

    VoucherDocumentContext doc = buildDocumentContext(voucher);
    byte[] pdf = pdfService.renderPdf(doc);

    EmailSenderService.Message.MessageBuilder builder = EmailSenderService.Message.builder()
        .replyTo(replyTo)
        .subject("VOUCHER - Nº " + voucher.getCode())
        .template("voucher/send-voucher")
        .eventType("voucher_send")
        .requestedById(currentUserProvider.getCurrentUser().userId())
        .data("voucher", doc.voucher())
        .data("clientCity", doc.clientCity())
        .data("clientPhone", doc.clientPhone())
        .data("clientEmail", doc.clientEmail())
        .data("remainingValue", doc.remainingValue())
        .data("company", doc.company())
        .data("importantInfo", doc.importantInfo())
        .attachment(EmailSenderService.Attachment.builder()
            .filename(voucher.getCode() + ".pdf")
            .resource(new ByteArrayResource(pdf))
            .contentType("application/pdf")
            .build());

    recipients.forEach(builder::to);
    emailSenderService.sendThymeleaf(builder.build());
  }

  public byte[] renderPdf(UUID id) {
    requireAuthority("Authenticated");
    Voucher voucher = getOrThrow(id);
    return pdfService.renderPdf(buildDocumentContext(voucher));
  }

  /** Monta tudo que o e-mail/PDF de voucher precisa além do {@link VoucherResponse} básico -
   *  cidade/telefone/e-mail do cliente (não fazem parte de AgentRefResponse pra não gerar N+1 na
   *  listagem de vouchers, que reusa o mesmo toResponse), o valor restante a pagar, os dados da
   *  empresa e os avisos de "informações importantes" configurados na tela de Voucher. Chamado
   *  pra um único voucher por vez (envio de e-mail/visualização de PDF), então o custo extra de
   *  resolver endereço/contato do cliente aqui não tem o mesmo problema de N+1 da listagem. */
  private VoucherDocumentContext buildDocumentContext(Voucher voucher) {
    VoucherResponse response = voucherService.toResponse(voucher);
    Agent client = voucher.getClient();

    BigDecimal total = response.totalPrice() == null ? BigDecimal.ZERO : response.totalPrice();
    BigDecimal advance = response.advanceValue() == null ? BigDecimal.ZERO : response.advanceValue();

    return new VoucherDocumentContext(
        response,
        resolveCity(client),
        resolvePhone(client),
        resolveEmail(client),
        total.subtract(advance),
        companySettingsService.getOrDefaultModel(),
        parseLines(configVoucherService.getOrCreate().getImportantInfo()));
  }

  private String resolveCity(Agent agent) {
    return agent.getAddresses().stream()
        .map(AgentAddress::getCity)
        .filter(city -> city != null)
        .findFirst()
        .map(city -> city.getName() + "/" + city.getState().getUf())
        .orElse(null);
  }

  private String resolvePhone(Agent agent) {
    return agent.getContacts().stream()
        .map(c -> (c.getCellphone() != null && !c.getCellphone().isBlank()) ? c.getCellphone() : c.getTelephone())
        .filter(phone -> phone != null && !phone.isBlank())
        .findFirst()
        .orElse(null);
  }

  private String resolveEmail(Agent agent) {
    return agent.getContacts().stream()
        .map(AgentContact::getEmail)
        .filter(email -> email != null && !email.isBlank())
        .findFirst()
        .orElse(null);
  }

  private List<String> parseLines(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split("\n")).map(String::trim).filter(s -> !s.isBlank()).toList();
  }

  private void sendChangeNotification(Voucher voucher, ConfigVoucher config) {
    List<String> recipients = voucher.getClient().getContacts().stream()
        .map(AgentContact::getEmail)
        .filter(email -> email != null && !email.isBlank())
        .toList();

    if (recipients.isEmpty()) {
      return;
    }

    VoucherResponse response = voucherService.toResponse(voucher);
    EmailSenderService.Message.MessageBuilder builder = EmailSenderService.Message.builder()
        .subject("Alteração de voucher - Nº " + voucher.getCode())
        .template("voucher/change-voucher")
        .eventType("voucher_change")
        .data("voucher", response)
        .data("emailBody", config.getEmailBody() == null ? "" : config.getEmailBody());

    recipients.forEach(builder::to);
    emailSenderService.sendThymeleaf(builder.build());
  }

  private Voucher getOrThrow(UUID id) {
    return voucherRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found: " + id));
  }

  private UUID currentUserId() {
    try {
      return UUID.fromString(currentUserProvider.requireUserId());
    } catch (IllegalStateException | IllegalArgumentException e) {
      return null;
    }
  }

  /** "Authenticated" só exige uma sessão válida (mesmo nível de {@code CheckSecurity.Authenticated}
   *  no legado para /to-view e /send-email) - sem exigir uma authority PERM_* específica. */
  private void requireAuthority(String permission) {
    if ("Authenticated".equals(permission)) {
      currentUserProvider.requireUserId();
      return;
    }
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
    }
  }
}
