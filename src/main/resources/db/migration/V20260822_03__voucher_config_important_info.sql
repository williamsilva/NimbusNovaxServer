-- Texto de "Informações importantes" exibido no rodapé do e-mail/PDF de voucher (avisos ao
-- cliente, ex.: proibição de alimentos, regras de remarcação/cancelamento) - configurável em
-- Configurações > Vouchers, uma linha por aviso (mesmo formato de texto livre de
-- ConfigVoucher.emailBody). Ver VoucherFlowService.buildDocumentContext.

ALTER TABLE voucher_configs ADD COLUMN important_info TEXT;

-- Seed com os avisos reais do voucher de referência do sistema legado - evita o rodapé sair em
-- branco antes de alguém preencher a tela manualmente; pode ser editado a qualquer momento.
UPDATE voucher_configs SET important_info =
'É proibida a entrada de alimentos e bebidas no parque;
Remarcações serão permitidas até 24 hs do dia anterior ao agendado para a visita;
Em caso de cancelamento será devolvido 70% do valor pago;
Caso o número de visitantes seja maior que o contratado, os excedentes terão a mesma tarifa individual que o restante do grupo, porém, sem direito a cortesia;
Estacionamento gratuito;
Ao motorista será oferecido almoço no restaurante para funcionários e entrada gratuita (horário do almoço 11:00h às 13:00h).'
WHERE config_key = 'VOUCHER_CHANGE';
