package com.nimbusnovax.common.notification.mail;

/** STRING enum (mesmo padrão de AddendumStatus/InstallmentStatus neste módulo) - diferente do
 *  Integer-code usado no NimbusAuth/CardsyncServer (EmailLogStatusEnum), só por consistência com
 *  o resto do NimbusNovax, não por necessidade técnica. */
public enum EmailLogStatus {
  SENT,
  FAILED
}
