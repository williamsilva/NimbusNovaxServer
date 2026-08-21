-- Motivo Cancelamento: descrição estava curta demais (50 caracteres, herdado 1:1 do sistema
-- legado) - aumentada para 150 a pedido do usuário.
ALTER TABLE cancellation_reasons ALTER COLUMN description TYPE VARCHAR(150);
