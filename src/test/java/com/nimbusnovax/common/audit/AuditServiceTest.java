package com.nimbusnovax.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.security.CurrentUserProvider.CurrentUser;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditServiceTest {

  private final AuditLogRepository repository = mock(AuditLogRepository.class);
  private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
  private final AuditService service = new AuditService(repository, currentUserProvider, new ObjectMapper());

  @BeforeEach
  void setUp() {
    when(currentUserProvider.getCurrentUser()).thenReturn(new CurrentUser("user-1", "user1", "User One"));
  }

  @Test
  void savesEntryWithSerializedPayloadsAndCurrentUser() {
    record Payload(String status) {
    }

    service.record("Addendum", "abc-123", "APPROVE", new Payload("PENDING"), new Payload("APPROVED"));

    var captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
    verify(repository).save(captor.capture());
    AuditLog saved = captor.getValue();

    assertThat(saved.getEntityName()).isEqualTo("Addendum");
    assertThat(saved.getEntityId()).isEqualTo("abc-123");
    assertThat(saved.getAction()).isEqualTo("APPROVE");
    assertThat(saved.getUserId()).isEqualTo("user-1");
    assertThat(saved.getDataBefore()).contains("PENDING");
    assertThat(saved.getDataAfter()).contains("APPROVED");
  }

  @Test
  void savesEntryWithNullPayloadsAsNull() {
    service.record("Installment", UUID.randomUUID().toString(), "RELEASE", null, null);

    var captor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getDataBefore()).isNull();
    assertThat(captor.getValue().getDataAfter()).isNull();
  }

  @Test
  void neverThrowsWhenRepositorySaveFails() {
    when(repository.save(any())).thenThrow(new RuntimeException("db down"));

    org.assertj.core.api.Assertions.assertThatCode(() -> service.record("Measurement", "id-1", "APPROVE", null, "x"))
        .doesNotThrowAnyException();
  }

  @Test
  void neverThrowsWhenCurrentUserLookupFails() {
    when(currentUserProvider.getCurrentUser()).thenThrow(new IllegalStateException("no auth context"));

    org.assertj.core.api.Assertions.assertThatCode(() -> service.record("Measurement", "id-1", "APPROVE", null, "x"))
        .doesNotThrowAnyException();
  }
}
