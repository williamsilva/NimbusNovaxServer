package com.nimbusnovax.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Registro de auditoria (PROJECT_SPEC.md seção 2/5/8 Fase 7) - "quem, quando, o quê, de/para qual
 * status" pra toda ação sensível (aprovação de aditivo/medição, liberação/pagamento de parcela).
 * Vive em common.audit (não em works) porque módulos futuros (tickets/tasks/actionplans) devem
 * reaproveitar essa mesma tabela/serviço em vez de duplicar auditoria por módulo.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_logs")
public class AuditLog {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "entity_name", nullable = false, length = 100)
  private String entityName;

  @Column(name = "entity_id", length = 100)
  private String entityId;

  @Column(nullable = false, length = 50)
  private String action;

  @Column(name = "user_id", length = 100)
  private String userId;

  /** JSON serializado (best-effort) - nulo quando o dado não estava disponível ou falhou ao serializar. */
  @Column(name = "data_before", columnDefinition = "text")
  private String dataBefore;

  @Column(name = "data_after", columnDefinition = "text")
  private String dataAfter;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant timestamp;
}
