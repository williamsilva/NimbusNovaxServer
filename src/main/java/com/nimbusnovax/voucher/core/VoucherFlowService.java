package com.nimbusnovax.voucher.core;

import com.nimbusnovax.administracao.model.AgentContact;
import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.repository.CancellationReasonRepository;
import com.nimbusnovax.common.notification.mail.EmailSenderService;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
import com.nimbusnovax.voucher.model.ConfigVoucher;
import com.nimbusnovax.voucher.model.Voucher;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import com.nimbusnovax.voucher.repository.VoucherRepository;
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
  private final VoucherPdfService pdfService;
  private final EmailSenderService emailSenderService;
  private final CurrentUserProvider currentUserProvider;

  public void confirm(UUID id) {
    requireAuthority("VOUCHERS_CHANGE");
    Voucher voucher = getOrThrow(id);
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
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Only a CONFIRMED voucher can be exchanged, current status: " + voucher.getStatusEnum());
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
            HttpStatus.NOT_FOUND, "Cancellation reason not found: " + cancellationReasonId));

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
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Voucher " + status + " cannot be sent");
    }

    List<String> recipients = voucher.getClient().getContacts().stream()
        .map(AgentContact::getEmail)
        .filter(email -> email != null && !email.isBlank())
        .toList();

    if (recipients.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unable to send email. Registered contactless customer");
    }

    String replyTo = voucher.getPromoter().getContacts().stream()
        .map(AgentContact::getEmail)
        .filter(email -> email != null && !email.isBlank())
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Complete your promoter registration, invalid contact"));

    VoucherResponse response = voucherService.toResponse(voucher);
    byte[] pdf = pdfService.renderPdf(response);

    EmailSenderService.Message.MessageBuilder builder = EmailSenderService.Message.builder()
        .replyTo(replyTo)
        .subject("VOUCHER - Nº " + voucher.getCode())
        .template("voucher/send-voucher")
        .eventType("voucher_send")
        .requestedById(currentUserProvider.getCurrentUser().userId())
        .data("voucher", response)
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
    return pdfService.renderPdf(voucherService.toResponse(voucher));
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
