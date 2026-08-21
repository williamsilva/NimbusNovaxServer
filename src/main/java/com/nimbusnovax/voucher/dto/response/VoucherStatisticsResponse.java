package com.nimbusnovax.voucher.dto.response;

import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import java.math.BigDecimal;
import java.util.UUID;

public final class VoucherStatisticsResponse {

  private VoucherStatisticsResponse() {
  }

  public record ByStatus(StatusVoucherEnum status, long total) {
  }

  public record Totals(
      long clientCount, long voucherCount, BigDecimal totalPrice, BigDecimal totalPriceTickets,
      BigDecimal totalPriceFoods) {
  }

  public record TopClient(UUID clientId, String clientName, long voucherCount, BigDecimal totalPrice) {
  }
}
