-- Módulo Voucher, dados migrados do sistema legado "Novax antigo" (MySQL: ws_voucher/ws_ticket/
-- ws_food/ws_config_voucher - ver V20260821_03/04 pros dados). status: 1=DEALING/2=CONFIRMED/
-- 3=EXCHANGED/4=OVERDUE/5=CALLED_OFF/6=NOT_CLOSED (StatusVoucherEnum) - mesmos códigos do legado,
-- preservados de propósito pra bater com os dados migrados sem tradução. type_person: 1=PHYSICAL/
-- 2=LEGAL (TypePersonEnum, já usado por Agent).

CREATE TABLE vouchers (
  id                      UUID           PRIMARY KEY,
  code                    VARCHAR(10)    NOT NULL UNIQUE,
  status                  INT            NOT NULL,
  type_person             INT            NOT NULL,
  number_of_visit         INT,
  total_price             NUMERIC(10,2)  NOT NULL DEFAULT 0,
  advance_value           NUMERIC(10,2)  NOT NULL DEFAULT 0,
  total_price_tickets     NUMERIC(10,2)  NOT NULL DEFAULT 0,
  total_price_foods       NUMERIC(10,2)  NOT NULL DEFAULT 0,
  visit_date              DATE,
  confirmation_date       TIMESTAMPTZ,
  cancellation_date       TIMESTAMPTZ,
  note                    VARCHAR(200),
  client_id               UUID           NOT NULL REFERENCES agents (id),
  promoter_id             UUID           NOT NULL REFERENCES agents (id),
  tour_guide_id           UUID           REFERENCES agents (id),
  cancellation_reason_id  UUID           REFERENCES cancellation_reasons (id),
  created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),
  updated_at              TIMESTAMPTZ,
  created_by_id           UUID,
  updated_by_id           UUID
);

CREATE INDEX vouchers_client_id_idx ON vouchers (client_id);
CREATE INDEX vouchers_promoter_id_idx ON vouchers (promoter_id);
CREATE INDEX vouchers_status_idx ON vouchers (status);
CREATE INDEX vouchers_visit_date_idx ON vouchers (visit_date);

CREATE TABLE voucher_tickets (
  id          UUID           PRIMARY KEY,
  voucher_id  UUID           NOT NULL REFERENCES vouchers (id) ON DELETE CASCADE,
  product_id  UUID           NOT NULL REFERENCES products (id),
  quantity    INT            NOT NULL,
  unit_price  NUMERIC(10,2)  NOT NULL,
  total_price NUMERIC(10,2)
);

CREATE INDEX voucher_tickets_voucher_id_idx ON voucher_tickets (voucher_id);

CREATE TABLE voucher_foods (
  id          UUID           PRIMARY KEY,
  voucher_id  UUID           NOT NULL REFERENCES vouchers (id) ON DELETE CASCADE,
  product_id  UUID           NOT NULL REFERENCES products (id),
  quantity    INT            NOT NULL,
  unit_price  NUMERIC(10,2)  NOT NULL,
  total_price NUMERIC(10,2)
);

CREATE INDEX voucher_foods_voucher_id_idx ON voucher_foods (voucher_id);

-- Linha única (config_key = 'VOUCHER_CHANGE', ver ConfigVoucherService.KEY) - dias para
-- expirar/cancelar, nº de vouchers pendentes permitido por cliente, notificação por e-mail.
-- notification_emails é uma divergência deliberada do legado: lá os destinatários do aviso de
-- vouchers vencidos eram usuários com uma permissão especial - aqui não há tabela de usuário
-- local (identidade 100% via NimbusAuth/JWT), então a lista de e-mails é configurada direto aqui.
CREATE TABLE voucher_configs (
  id                        UUID          PRIMARY KEY,
  config_key                VARCHAR(50)   NOT NULL UNIQUE,
  sender_mail               BOOLEAN       NOT NULL DEFAULT true,
  days_to_expire            INT           NOT NULL,
  days_to_cancel            INT           NOT NULL,
  number_pending_vouchers   INT           NOT NULL,
  email_body                TEXT,
  notification_emails       VARCHAR(500),
  created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_at                TIMESTAMPTZ,
  created_by_id             UUID,
  updated_by_id             UUID
);
