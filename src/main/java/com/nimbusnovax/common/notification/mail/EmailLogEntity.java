package com.nimbusnovax.common.notification.mail;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Auditoria de envio - mesmo papel do EmailLogEntity no NimbusAuth/CardsyncServer. Sem FK pra
 *  usuário (requestedById é só o id/sub do JWT, guardado como texto - NimbusNovaxServer não tem
 *  tabela própria de usuário). */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_log")
public class EmailLogEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "event_type", nullable = false, length = 60)
  private String eventType;

  /** TODOS os destinatários do envio, separados por ", " (antes só o primeiro - ver
   *  V36__email_log_recipients_and_body.sql). */
  @Column(nullable = false, columnDefinition = "text")
  private String recipients;

  @Column(nullable = false, length = 300)
  private String subject;

  @Column(nullable = false, length = 200)
  private String template;

  /** Corpo HTML renderizado (Thymeleaf) no momento do envio - nulo em registros anteriores a
   *  V36__email_log_recipients_and_body.sql. */
  @Column(columnDefinition = "text")
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private EmailLogStatus status;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "requested_by_id", length = 100)
  private String requestedById;

  @CreationTimestamp
  @Column(name = "sent_at", nullable = false, updatable = false)
  private Instant sentAt;
}
