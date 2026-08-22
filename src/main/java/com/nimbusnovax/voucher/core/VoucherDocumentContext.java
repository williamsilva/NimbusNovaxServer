package com.nimbusnovax.voucher.core;

import com.nimbusnovax.common.company.CompanySettingsModel;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
import java.math.BigDecimal;
import java.util.List;

/** Tudo que o e-mail/PDF do voucher precisa além do próprio {@link VoucherResponse} (que fica
 *  enxuto de propósito - é reusado pela listagem/edição, então cidade/telefone do cliente e afins
 *  não entram nele pra não gerar N+1 ao montar a listagem inteira de vouchers). Montado uma vez
 *  por {@code VoucherFlowService.buildDocumentContext} e consumido tanto pelo e-mail
 *  ({@code send-voucher.html}) quanto pelo PDF anexado ({@code voucher-pdf.html}). */
record VoucherDocumentContext(
    VoucherResponse voucher,
    String clientDocument,
    String clientCity,
    String clientPhone,
    String clientEmail,
    BigDecimal remainingValue,
    CompanySettingsModel company,
    List<String> importantInfo) {
}
