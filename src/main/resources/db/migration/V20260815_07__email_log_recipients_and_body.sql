-- email_log guardava só o PRIMEIRO destinatário do envio (EmailLogService.firstRecipient) e nunca
-- o corpo renderizado - insuficiente pra uma auditoria de e-mail de verdade (tela nova em
-- Configurações > Auditoria de E-mail). Nenhuma tela/endpoint consumia esta tabela até agora (só
-- gravação existia), então é seguro renomear a coluna em vez de duplicar.
ALTER TABLE email_log RENAME COLUMN recipient TO recipients;
ALTER TABLE email_log ALTER COLUMN recipients TYPE TEXT;

-- Corpo HTML renderizado (Thymeleaf) no momento do envio - nulo em registros anteriores a esta
-- migration (não há como reconstruir retroativamente, o body nunca foi persistido antes).
ALTER TABLE email_log ADD COLUMN body TEXT;

CREATE INDEX email_log_status_idx ON email_log (status);
CREATE INDEX email_log_event_type_idx ON email_log (event_type);
