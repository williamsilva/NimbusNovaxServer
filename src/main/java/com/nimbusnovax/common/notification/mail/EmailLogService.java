package com.nimbusnovax.common.notification.mail;

import com.nimbusnovax.common.web.FilterSupport;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailLogService {

  private final EmailLogRepository repository;

  /** REQUIRES_NEW - alguns chamadores (ex.: VoucherScheduledTasks.warnExpiredVouchers) disparam o
   *  envio de dentro de uma transação @Transactional(readOnly = true): sem propagação própria,
   *  este save() participa dessa transação de leitura e é descartado silenciosamente no commit
   *  (Hibernate nem chega a fazer flush de uma sessão só-leitura) - o e-mail sai de verdade, mas a
   *  auditoria em email_log fica muda, sem nenhum erro visível. Mesmo motivo de logError abaixo. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logSent(EmailSenderService.Message message, String body) {
    repository.save(EmailLogEntity.builder()
        .eventType(message.getEventType())
        .recipients(joinRecipients(message))
        .subject(message.getSubject())
        .template(message.getTemplate())
        .body(body)
        .status(EmailLogStatus.SENT)
        .requestedById(message.getRequestedById())
        .build());
  }

  /** REQUIRES_NEW - mesmo motivo do EmailLogService.logError no NimbusAuth: o registro do erro
   *  precisa sobreviver mesmo que a transação que tentou enviar o e-mail seja revertida depois. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void logError(EmailSenderService.Message message, String body, Exception ex) {
    repository.save(EmailLogEntity.builder()
        .eventType(message.getEventType())
        .recipients(joinRecipients(message))
        .subject(message.getSubject())
        .template(message.getTemplate())
        .body(body)
        .status(EmailLogStatus.FAILED)
        .errorMessage(truncate(ex.getMessage(), 1000))
        .requestedById(message.getRequestedById())
        .build());
  }

  /**
   * Listagem paginada/filtrada/ordenada (tela Configurações &gt; Auditoria de E-mail, no padrão
   * CardSync de listagem) - mesmo estilo de {@code AddendumApprovalService.search}: busca tudo em
   * memória (volume esperado pequeno/médio pra este app, mesma premissa das demais listagens) e
   * filtra/ordena/pagina depois. Só os e-mails de negócio próprios (email_log, esta tabela) - não
   * mescla mais com os de convite/reset de senha do NimbusAuth (removido: essa tela é a auditoria
   * do NimbusNovax, e-mail de outro app só confundia quem está analisando os próprios envios).
   */
  @Transactional(readOnly = true)
  public PageResponse<EmailLogModel> search(SearchRequest request) {
    List<EmailLogModel> all = repository.findAll().stream().map(this::toModel).toList();
    List<EmailLogModel> sorted = sortLogs(filterLogs(all, request), request);

    int page = request.page() == null ? 0 : Math.max(0, request.page());
    int size = request.size() == null || request.size() <= 0 ? 20 : request.size();
    int from = Math.min(page * size, sorted.size());
    int to = Math.min(from + size, sorted.size());

    return PageResponse.of(sorted.subList(from, to), page, size, sorted.size());
  }

  private List<EmailLogModel> filterLogs(List<EmailLogModel> items, SearchRequest request) {
    Map<String, Object> tableFilters = request.tableFilters();
    Map<String, Object> advanced = request.advanced();

    String recipients = FilterSupport.textFilter(tableFilters, advanced, "recipients", "recipients");
    String subject = FilterSupport.textFilter(tableFilters, advanced, "subject", "subject");
    List<String> eventTypes = FilterSupport.listFilter(tableFilters, advanced, "eventType", "eventType");
    List<String> statuses = FilterSupport.listFilter(tableFilters, advanced, "status", "status");
    Instant[] sentAtRange = FilterSupport.periodInstantRange(tableFilters, advanced, "sentAt", "periodSentAt", "sentAt");
    String global = request.globalFilter();

    return items.stream()
        .filter(e -> FilterSupport.containsIgnoreCase(e.recipients(), recipients))
        .filter(e -> FilterSupport.containsIgnoreCase(e.subject(), subject))
        .filter(e -> eventTypes.isEmpty() || eventTypes.contains(e.eventType()))
        .filter(e -> statuses.isEmpty() || statuses.contains(e.status().name()))
        .filter(e -> FilterSupport.withinRange(e.sentAt(), sentAtRange))
        .filter(e -> global == null || global.isBlank()
            || FilterSupport.containsIgnoreCase(e.recipients(), global)
            || FilterSupport.containsIgnoreCase(e.subject(), global))
        .toList();
  }

  private List<EmailLogModel> sortLogs(List<EmailLogModel> items, SearchRequest request) {
    SearchRequest.SortItem sortItem =
        (request.sort() == null || request.sort().isEmpty()) ? null : request.sort().get(0);
    String field = sortItem != null && sortItem.field() != null ? sortItem.field() : "sentAt";
    // sem sort explícito -> mais recentes primeiro
    boolean desc = sortItem != null && sortItem.order() != null ? sortItem.order() < 0 : true;

    Comparator<EmailLogModel> comparator = switch (field) {
      case "recipients" -> Comparator.comparing(e -> orEmpty(e.recipients()), String.CASE_INSENSITIVE_ORDER);
      case "subject" -> Comparator.comparing(e -> orEmpty(e.subject()), String.CASE_INSENSITIVE_ORDER);
      case "eventType" -> Comparator.comparing(e -> orEmpty(e.eventType()), String.CASE_INSENSITIVE_ORDER);
      case "status" -> Comparator.comparing(e -> e.status().name());
      default -> Comparator.comparing(e -> orEpoch(e.sentAt()));
    };

    if (desc) {
      comparator = comparator.reversed();
    }
    return items.stream().sorted(comparator).toList();
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  private static Instant orEpoch(Instant value) {
    return value == null ? Instant.EPOCH : value;
  }

  private EmailLogModel toModel(EmailLogEntity e) {
    return new EmailLogModel(
        e.getId(), e.getEventType(), e.getRecipients(), e.getSubject(), e.getTemplate(),
        e.getBody(), e.getStatus(), e.getErrorMessage(), e.getRequestedById(), e.getSentAt());
  }

  private String joinRecipients(EmailSenderService.Message message) {
    if (message.getRecipients() == null || message.getRecipients().isEmpty()) {
      return "unknown";
    }
    return String.join(", ", message.getRecipients());
  }

  private String truncate(String value, int max) {
    if (value == null) return null;
    return value.length() <= max ? value : value.substring(0, max);
  }
}
