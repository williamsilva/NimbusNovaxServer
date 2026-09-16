package com.nimbusnovax.voucher.dto.request;

import com.nimbusnovax.voucher.model.enums.PaymentMethodEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code status} não faz parte do request de propósito - só os endpoints de fluxo
 *  (VoucherFlowController) mudam o status de um voucher; create sempre nasce DEALING e update
 *  nunca altera status (ver VoucherService). {@code advanceValue} (valor único de "Depósito") saiu
 *  daqui - o pagamento antecipado agora é itemizado por forma de pagamento em
 *  {@code advancePayments}, com o total recalculado a partir dela (ver Voucher.calculateTotalPrice,
 *  mesmo esquema de totalPriceTickets/totalPriceFoods a partir de tickets/foods). */
public record VoucherRequest(
    String note,
    @NotNull LocalDate visitDate,
    @NotNull UUID clientId,
    @NotNull UUID promoterId,
    UUID tourGuideId,
    @Valid List<ItemRequest> tickets,
    @Valid List<ItemRequest> foods,
    @Valid List<AdvancePaymentRequest> advancePayments) {

  public record ItemRequest(@NotNull UUID productId, @NotNull Integer quantity, BigDecimal unitPrice) {
  }

  public record AdvancePaymentRequest(@NotNull PaymentMethodEnum paymentMethod, @NotNull BigDecimal amount) {
  }
}
