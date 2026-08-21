package com.nimbusnovax.voucher.dto.response;

import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VoucherResponse(
    UUID id,
    String code,
    StatusVoucherEnum status,
    TypePersonEnum typePerson,
    String note,
    LocalDate visitDate,
    Integer numberOfVisit,
    BigDecimal totalPrice,
    BigDecimal advanceValue,
    BigDecimal totalPriceTickets,
    BigDecimal totalPriceFoods,
    Instant confirmationDate,
    Instant cancellationDate,
    AgentRefResponse client,
    AgentRefResponse promoter,
    AgentRefResponse tourGuide,
    CancellationReasonRefResponse cancellationReason,
    List<ItemResponse> tickets,
    List<ItemResponse> foods,
    Instant createdAt,
    Instant updatedAt) {

  public record AgentRefResponse(UUID id, String name, String document) {
  }

  public record CancellationReasonRefResponse(UUID id, String name) {
  }

  public record ItemResponse(
      UUID id, UUID productId, String productName, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
  }
}
