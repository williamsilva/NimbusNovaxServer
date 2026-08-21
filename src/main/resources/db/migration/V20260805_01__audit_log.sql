-- Fase 7: log de auditoria para ações sensíveis (aprovação de aditivo/medição, liberação e
-- pagamento de parcela) - com.nimbusnovax.common.audit, reaproveitável pelos módulos futuros.
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY,
  entity_name VARCHAR(100) NOT NULL,
  entity_id VARCHAR(100),
  action VARCHAR(50) NOT NULL,
  user_id VARCHAR(100),
  data_before TEXT,
  data_after TEXT,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX audit_logs_entity_idx ON audit_logs (entity_name, entity_id);
CREATE INDEX audit_logs_timestamp_idx ON audit_logs (timestamp);
