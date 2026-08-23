package com.nimbusnovax.common.notification.mail;

import com.nimbusnovax.common.web.SearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailLogService {

  private final EmailLogRepository repository;
  private final EmailLogSpecs emailLogSpecs;

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

  /** Filtro/ordenação/paginação reais no banco via {@link EmailLogSpecs} (Specification) - ver
   *  {@link com.nimbusnovax.voucher.core.VoucherSpecs} para o padrão. Só os e-mails de negócio
   *  próprios (email_log, esta tabela) - não mescla com os de convite/reset de senha do
   *  NimbusAuth. */
  @Transactional(readOnly = true)
  public Page<EmailLogEntity> search(SearchRequest request, Pageable pageable) {
    Specification<EmailLogEntity> spec = emailLogSpecs.fromRequest(request);
    return repository.findAll(spec, pageable);
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
