package com.nimbusnovax.common.audit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Intercepta métodos anotados com @Auditable, deixa a ação de negócio rodar normalmente
 * (proceed() primeiro) e só então grava a auditoria com o resultado - se a ação lançar exceção,
 * proceed() propaga normalmente e nada é auditado (não existe "tentativa" auditada, só sucesso).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

  private final AuditService auditService;

  @Around("@annotation(auditable)")
  public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
    Object result = joinPoint.proceed();

    try {
      Object[] args = joinPoint.getArgs();
      String entityId = args.length > 0 && args[0] != null ? String.valueOf(args[0]) : null;
      auditService.record(auditable.entityName(), entityId, auditable.action(), null, result);
    } catch (Exception auditFailure) {
      // AuditService.record() já engole os próprios erros - esse catch é só uma rede de segurança
      // extra (ex.: falha ao ler joinPoint.getArgs()) pra garantir que auditoria nunca quebra a ação.
      log.warn("Unexpected failure while auditing {}.{}", auditable.entityName(), auditable.action(), auditFailure);
    }

    return result;
  }
}
