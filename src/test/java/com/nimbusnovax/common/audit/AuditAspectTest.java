package com.nimbusnovax.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;

class AuditAspectTest {

  private final AuditService auditService = mock(AuditService.class);
  private final AuditAspect aspect = new AuditAspect(auditService);

  @Test
  void recordsAuditAfterProceedSucceedsUsingFirstArgAsEntityId() throws Throwable {
    Auditable auditable = annotationFor("approve");
    UUID id = UUID.randomUUID();
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    when(pjp.getArgs()).thenReturn(new Object[] { id, "extra" });
    when(pjp.proceed()).thenReturn("result");

    Object result = aspect.audit(pjp, auditable);

    assertThat(result).isEqualTo("result");
    verify(auditService).record("Addendum", id.toString(), "APPROVE", null, "result");
  }

  @Test
  void doesNotRecordAndPropagatesExceptionWhenProceedThrows() throws Throwable {
    Auditable auditable = annotationFor("approve");
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    when(pjp.getArgs()).thenReturn(new Object[] { UUID.randomUUID() });
    when(pjp.proceed()).thenThrow(new IllegalStateException("business rule violated"));

    assertThatThrownBy(() -> aspect.audit(pjp, auditable)).isInstanceOf(IllegalStateException.class);
    verify(auditService, never()).record(any(), any(), any(), any(), any());
  }

  @Test
  void neverPropagatesFailureFromAuditServiceItself() throws Throwable {
    Auditable auditable = annotationFor("approve");
    ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
    when(pjp.getArgs()).thenReturn(new Object[] { UUID.randomUUID() });
    when(pjp.proceed()).thenReturn("result");
    doThrow(new RuntimeException("boom")).when(auditService).record(any(), any(), any(), any(), any());

    Object result = aspect.audit(pjp, auditable);

    assertThat(result).isEqualTo("result");
  }

  private Auditable annotationFor(String methodName) throws NoSuchMethodException {
    Method method = AnnotatedSample.class.getDeclaredMethod(methodName, UUID.class);
    return method.getAnnotation(Auditable.class);
  }

  private static final class AnnotatedSample {
    @Auditable(entityName = "Addendum", action = "APPROVE")
    void approve(UUID id) {
    }
  }
}
