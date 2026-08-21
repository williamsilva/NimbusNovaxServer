package com.nimbusnovax.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método de service como uma ação sensível a auditar (PROJECT_SPEC.md seção 2). O
 * primeiro parâmetro do método precisa ser o id da entidade (UUID) - convenção seguida por todos
 * os métodos de decisão/transição de estado do módulo works (approve/reject/release/markAsPaid).
 * O aspecto (AuditAspect) grava dataAfter com o retorno do método (serializado); dataBefore fica
 * nulo de propósito - capturar o "antes" de forma genérica via AOP exigiria uma estratégia de
 * snapshot por entidade (especulativa demais pro estágio atual), e não há perda real de
 * informação aqui: toda transição auditada só é possível a partir de um estado fixo e já
 * validado pelo próprio service (ex.: approveAddendum só aceita Addendum PENDING) - a ação e o
 * "depois" já implicam o "antes".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {

  String entityName();

  String action();
}
