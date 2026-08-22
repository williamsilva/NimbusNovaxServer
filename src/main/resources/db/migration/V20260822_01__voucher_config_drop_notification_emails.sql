-- Remove a lista de e-mails configurada manualmente pra aviso de vouchers vencidos: os
-- destinatários agora são resolvidos consultando o NimbusAuth (GET /internal/users/permissions)
-- por quem tem a permissão VOUCHER_NOTIFICATION, em vez de manter uma lista de e-mails aqui,
-- desincronizada do catálogo real de usuários/grupos (ver VoucherScheduledTasks).

ALTER TABLE voucher_configs DROP COLUMN notification_emails;
