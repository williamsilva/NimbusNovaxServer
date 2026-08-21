package com.nimbusnovax.common.notification.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusnovax.common.security.NimbusAuthAdminClient;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmailLogServiceTest {

  private final EmailLogRepository repository = mock(EmailLogRepository.class);
  private final NimbusAuthAdminClient nimbusAuthAdminClient = mock(NimbusAuthAdminClient.class);
  private final EmailLogService service = new EmailLogService(repository, nimbusAuthAdminClient);

  @BeforeEach
  void setUp() {
    // Nada de convite/reset do NimbusAuth por padrão - testes que exercitam essa mescla
    // sobrescrevem este stub geral (Mockito usa o mais recente que casar).
    when(nimbusAuthAdminClient.searchEmailLogs(any())).thenReturn(List.of());
  }

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

  @Test
  void searchFiltersByRecipientsSubjectStatusAndEventType() {
    EmailLogEntity sentToA = entityOf("a@example.com", "Aditivo aprovado", EmailLogStatus.SENT, "addendum_approved");
    EmailLogEntity failedToB = entityOf("b@example.com", "Pagamento liberado", EmailLogStatus.FAILED, "payment_released");
    when(repository.findAll()).thenReturn(List.of(sentToA, failedToB));

    SearchRequest request = new SearchRequest(0, 20, null, Map.of(), null,
        Map.of("status", List.of("FAILED")));

    PageResponse<EmailLogModel> page = service.search(request, "token");

    assertThat(page._embedded().content()).hasSize(1);
    assertThat(page._embedded().content().get(0).recipients()).isEqualTo("b@example.com");
    assertThat(page.page().totalElements()).isEqualTo(1);
  }

  @Test
  void searchDefaultsToMostRecentFirstWhenNoSortGiven() {
    EmailLogEntity older = entityOf("a@example.com", "Mais antigo", EmailLogStatus.SENT, "addendum_approved");
    older.setSentAt(Instant.parse("2026-01-01T00:00:00Z"));
    EmailLogEntity newer = entityOf("a@example.com", "Mais novo", EmailLogStatus.SENT, "addendum_approved");
    newer.setSentAt(Instant.parse("2026-02-01T00:00:00Z"));
    when(repository.findAll()).thenReturn(List.of(older, newer));

    SearchRequest request = new SearchRequest(0, 20, null, Map.of(), null, Map.of());

    PageResponse<EmailLogModel> page = service.search(request, "token");

    assertThat(page._embedded().content()).extracting(EmailLogModel::subject)
        .containsExactly("Mais novo", "Mais antigo");
  }

  @Test
  void searchMergesInviteAndPasswordResetEmailsFromNimbusAuth() {
    when(repository.findAll()).thenReturn(List.of());
    when(nimbusAuthAdminClient.searchEmailLogs("token")).thenReturn(List.of(
        new NimbusAuthAdminClient.RawEmailLog(
            java.util.UUID.randomUUID(), "SENT", "PASSWORD_RESET", "Redefinição de senha",
            "mail/reset-senha-mail.html", "user@acquamania.com.br", null, "<html>corpo</html>",
            Instant.parse("2026-01-01T00:00:00Z"))));

    SearchRequest request = new SearchRequest(0, 20, null, Map.of(), null, Map.of());

    PageResponse<EmailLogModel> page = service.search(request, "token");

    assertThat(page._embedded().content()).hasSize(1);
    EmailLogModel merged = page._embedded().content().get(0);
    assertThat(merged.eventType()).isEqualTo("password_reset");
    assertThat(merged.status()).isEqualTo(EmailLogStatus.SENT);
    assertThat(merged.recipients()).isEqualTo("user@acquamania.com.br");
    assertThat(merged.body()).isEqualTo("<html>corpo</html>");
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
