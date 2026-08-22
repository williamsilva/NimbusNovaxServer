package com.nimbusnovax.voucher.core;

import java.time.LocalDate;

/** Linha da tabela do e-mail de aviso de vouchers vencidos (ver
 *  VoucherScheduledTasks.warnExpiredVouchers) - resolvida uma vez em Java (nome do cliente/
 *  promotor, dias em atraso) em vez de passar a entidade {@code Voucher} crua pro template, que
 *  teria que navegar relações lazy e não teria como calcular "dias em atraso" sem lógica embutida
 *  no HTML. */
record ExpiredVoucherWarningItem(
    String code, String clientName, String promoterName, LocalDate visitDate, long daysOverdue) {
}
