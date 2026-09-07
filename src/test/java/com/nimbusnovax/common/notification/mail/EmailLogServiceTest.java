package com.nimbusnovax.common.notification.mail;

import com.nimbussystems.commons.notification.mail.EmailSenderService;

import com.nimbussystems.commons.notification.mail.EmailLogStatus;

import com.nimbussystems.commons.notification.mail.EmailLogEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbussystems.commons.web.SearchRequest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class EmailLogServiceTest {

  private final EmailLogRepository repository = mock(EmailLogRepository.class);
  private final EmailLogSpecs emailLogSpecs = mock(EmailLogSpecs.class);
  private final EmailLogService service = new EmailLogService(repository, emailLogSpecs);

  @Test
  void logSentJoinsAllRecipientsAndStoresBody() {
    Set<String> recipients = new LinkedHashSet<>(List.of("a@example.com", "b@example.com"));
    EmailSenderService.Message message = EmailSenderService.Message.builder()
        .recipients(recipients)
        .subject("Assunto")
        .template("mail/addendum_approved")
        .eventType("addendum_approved")
        .requestedById("user-1")
        .build();

    when(repository.save(any(EmailLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    service.logSent(message, "<html>corpo</html>");

    ArgumentCaptor<EmailLogEntity> captor = ArgumentCaptor.forClass(EmailLogEntity.class);
    verify(repository).save(captor.capture());
    EmailLogEntity saved = captor.getValue();
    assertThat(saved.getRecipients()).isEqualTo("a@example.com, b@example.com");
    assertThat(saved.getBody()).isEqualTo("<html>corpo</html>");
    assertThat(saved.getStatus()).isEqualTo(EmailLogStatus.SENT);
  }

  @Test
  void logSentFallsBackToUnknownWhenNoRecipients() {
    EmailSenderService.Message message = EmailSenderService.Message.builder()
        .subject("Assunto")
        .template("mail/ticket_target")
        .eventType("ticket_target")
        .build();

    when(repository.save(any(EmailLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    service.logSent(message, "corpo");

    ArgumentCaptor<EmailLogEntity> captor = ArgumentCaptor.forClass(EmailLogEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getRecipients()).isEqualTo("unknown");
  }

  @Test
  void logErrorTruncatesMessageAndKeepsBody() {
    EmailSenderService.Message message = EmailSenderService.Message.builder()
        .recipients(Set.of("a@example.com"))
        .subject("Assunto")
        .template("mail/payment_released")
        .eventType("payment_released")
        .build();

    when(repository.save(any(EmailLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    String longMessage = "x".repeat(1200);
    service.logError(message, "<html>corpo</html>", new RuntimeException(longMessage));

    ArgumentCaptor<EmailLogEntity> captor = ArgumentCaptor.forClass(EmailLogEntity.class);
    verify(repository).save(captor.capture());
    EmailLogEntity saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(EmailLogStatus.FAILED);
    assertThat(saved.getErrorMessage()).hasSize(1000);
    assertThat(saved.getBody()).isEqualTo("<html>corpo</html>");
  }

  /** search() virou uma delegação fina pra EmailLogSpecs (monta a Specification a partir do
   *  SearchRequest) + repository.findAll(spec, pageable) - filtro/ordenação/paginação de verdade
   *  agora acontecem no banco, não em memória (ver EmailLogSpecs). Cobrir a lógica de filtro em si
   *  exigiria um teste de integração com banco de verdade (Specification/Criteria API não executa
   *  contra mocks) - fora do escopo deste teste unitário, que só garante a delegação certa. */
  @Test
  void searchDelegatesToEmailLogSpecsAndRepository() {
    SearchRequest request = new SearchRequest(0, 20, null, Map.of(), null, Map.of("status", List.of("FAILED")));
    Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "sentAt"));

    @SuppressWarnings("unchecked")
    Specification<EmailLogEntity> spec = mock(Specification.class);
    when(emailLogSpecs.fromRequest(request)).thenReturn(spec);

    EmailLogEntity failedToB = entityOf("b@example.com", "Pagamento liberado", EmailLogStatus.FAILED, "payment_released");
    Page<EmailLogEntity> expected = new PageImpl<>(List.of(failedToB), pageable, 1);
    when(repository.findAll(spec, pageable)).thenReturn(expected);

    Page<EmailLogEntity> result = service.search(request, pageable);

    assertThat(result.getContent()).containsExactly(failedToB);
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  private EmailLogEntity entityOf(String recipients, String subject, EmailLogStatus status, String eventType) {
    return EmailLogEntity.builder()
        .eventType(eventType)
        .recipients(recipients)
        .subject(subject)
        .template("mail/x")
        .status(status)
        .sentAt(Instant.now())
        .build();
  }
}
