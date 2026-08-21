-- Config de e-mail própria do NimbusNovax (com.nimbusnovax.common.notification.mail) - só a
-- infraestrutura (FAKE/SMTP/API_KEY + tela de Configurações), nenhum evento de negócio dispara
-- envio ainda. Mesmo shape de cs_email_settings (CardsyncServer) menos chargeback_recipients
-- (conceito que não existe no NimbusNovax).

CREATE TABLE email_settings (
  id             UUID         PRIMARY KEY,
  impl           VARCHAR(10),
  from_name      VARCHAR(255),
  from_email     VARCHAR(255),
  brevo_api_key  VARCHAR(500),
  brevo_base_url VARCHAR(255),
  brevo_port     INT,
  brevo_username VARCHAR(255),
  smtp_host      VARCHAR(255),
  smtp_port      INT,
  smtp_username  VARCHAR(255),
  smtp_password  VARCHAR(500),
  smtp_auth      BOOLEAN,
  smtp_starttls  BOOLEAN,
  smtp_ssl       BOOLEAN,
  created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Auditoria de envio (sucesso/erro) - mesmo papel de cs_email_log/nb_email_log.
CREATE TABLE email_log (
  id              UUID         PRIMARY KEY,
  event_type      VARCHAR(60)  NOT NULL,
  recipient       VARCHAR(320) NOT NULL,
  subject         VARCHAR(300) NOT NULL,
  template        VARCHAR(200) NOT NULL,
  status          VARCHAR(10)  NOT NULL,
  error_message   VARCHAR(1000),
  requested_by_id VARCHAR(100),
  sent_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX email_log_sent_at_idx ON email_log (sent_at);
