-- "Depósito" vira "Pagamento Antecipado" com múltiplas formas de pagamento - voucher_advance_payments
-- é N:1 com vouchers, mesmo papel de voucher_tickets/voucher_foods. payment_method: 1=BANK_DEPOSIT/
-- 2=CREDIT_CARD/3=DEBIT_CARD/4=PIX/5=CASH/6=CHECK/7=BANK_SLIP/8=OTHER (PaymentMethodEnum, conceito
-- novo sem equivalente legado). vouchers.advance_value continua existindo como total calculado (mesmo
-- papel de total_price_tickets/total_price_foods), agora somado a partir desta tabela em vez de
-- informado direto no formulário - backfill abaixo migra o valor único antigo pra uma linha
-- BANK_DEPOSIT (única forma que o campo "Depósito" representava até aqui).

CREATE TABLE voucher_advance_payments (
  id              UUID           PRIMARY KEY,
  voucher_id      UUID           NOT NULL REFERENCES vouchers (id) ON DELETE CASCADE,
  payment_method  INT            NOT NULL,
  amount          NUMERIC(10,2)  NOT NULL
);

CREATE INDEX voucher_advance_payments_voucher_id_idx ON voucher_advance_payments (voucher_id);

INSERT INTO voucher_advance_payments (id, voucher_id, payment_method, amount)
SELECT gen_random_uuid(), id, 1, advance_value
FROM vouchers
WHERE advance_value <> 0;
