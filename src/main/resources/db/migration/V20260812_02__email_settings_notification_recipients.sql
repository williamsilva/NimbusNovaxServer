-- Campo "Destinatários de notificação" da tela Configurações > E-mail (chargebackRecipients no
-- wire format/entidade - nome mantido por continuidade, ver EmailSettingsEntity) já existia na
-- API/tela desde a criação do módulo de e-mail do NimbusNovax, mas nunca foi persistido de
-- propósito (sem uso real até agora).

ALTER TABLE email_settings ADD COLUMN chargeback_recipients VARCHAR(2000);
