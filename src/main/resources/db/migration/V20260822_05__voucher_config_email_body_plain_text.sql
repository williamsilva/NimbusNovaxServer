-- V20260822_04 salvou o texto de agradecimento com marcações HTML (<p>...</p>) porque o template
-- ainda usava th:utext (HTML não escapado) - a tela de Configuração de Vouchers só tem um
-- <textarea> simples (sem editor rich-text, ver comentário em VoucherConfigPageComponent), então
-- quem fosse editar veria as tags cruas. O template agora usa th:text (texto puro, preservando
-- quebra de linha via CSS white-space:pre-line) - mesmo critério de "Informações importantes",
-- então o valor salvo também precisa virar texto puro (linha em branco separa parágrafos).

UPDATE voucher_configs SET email_body =
'Muito obrigado pela sua visita! Esperamos que tenha aproveitado cada momento da experiência conosco.

Foi um prazer recebê-lo(a), e ficaremos muito felizes em vê-lo(a) novamente em uma próxima oportunidade!'
WHERE config_key = 'VOUCHER_CHANGE';
