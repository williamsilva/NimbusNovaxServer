-- Texto de agradecimento exibido no e-mail de troca de voucher (voucher/change-voucher.html),
-- enviado quando o voucher é marcado como acessado (visita realizada) - ver
-- VoucherFlowService.sendChangeNotification. Substitui o texto de teste digitado durante o
-- desenvolvimento desta tela ("Teste envio de email ao trocar voucher"); pode ser editado a
-- qualquer momento em Configurações > Vouchers > Texto da notificação.

UPDATE voucher_configs SET email_body =
'<p>Muito obrigado pela sua visita! Esperamos que tenha aproveitado cada momento da experiência conosco.</p><p>Foi um prazer recebê-lo(a), e ficaremos muito felizes em vê-lo(a) novamente em uma próxima oportunidade!</p>'
WHERE config_key = 'VOUCHER_CHANGE';
