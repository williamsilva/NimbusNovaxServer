-- Até aqui só existia uma lista de destinatários de notificação (chargeback_recipients, nome
-- herdado do CardSyncWeb - ver V16__email_settings_notification_recipients.sql), usada só pelo
-- e-mail de parcela liberada (PaymentNotificationService). Agora que passa a existir um segundo
-- evento com lista própria (aditivo aprovado, ver AddendumNotificationService), renomeia a coluna
-- existente pra deixar explícito qual evento cada uma alimenta, em vez de manter um nome legado
-- genérico ao lado de um nome novo específico.
ALTER TABLE email_settings RENAME COLUMN chargeback_recipients TO payment_recipients;
ALTER TABLE email_settings ADD COLUMN addendum_recipients VARCHAR(2000);
