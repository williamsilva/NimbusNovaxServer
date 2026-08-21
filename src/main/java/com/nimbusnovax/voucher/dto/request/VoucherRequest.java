package com.nimbusnovax.voucher.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code status} não faz parte do request de propósito - só os endpoints de fluxo
 *  (VoucherFlowController) mudam o status de um voucher; create sempre nasce DEALING e update
 *  nunca altera status (ver VoucherService). */
public record VoucherRequest(
    String note,
    @NotNull LocalDate visitDate,
    BigDecimal advanceValue,
    @NotNull UUID clientId,
    @NotNull UUID promoterId,
    UUID tourGuideId,
    @Valid List<ItemRequest> tickets,
    @Valid List<ItemRequest> foods) {

  public record ItemRequest(@NotNull UUID productId, @NotNull Integer quantity, BigDecimal unitPrice) {
  }
}
