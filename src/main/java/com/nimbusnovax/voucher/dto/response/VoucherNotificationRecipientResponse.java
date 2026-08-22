package com.nimbusnovax.voucher.dto.response;

/** Usuário que recebe o aviso diário de vouchers vencidos (tem a permissão VOUCHER_NOTIFICATION no
 *  NimbusAuth) - só leitura, exibido na tela de Configuração de Vouchers pra quem administra saber
 *  quem está recebendo o aviso; a permissão em si é concedida/revogada no NimbusAuth (grupo
 *  NOTIFICAÇÕES), não aqui (ver ConfigVoucherService#notificationRecipients). */
public record VoucherNotificationRecipientResponse(String name, String username) {
}
