package com.nimbusnovax.common.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  /** Reservado pra uma futura tela de histórico de auditoria por entidade - não usado ainda. */
  List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, String entityId);
}
