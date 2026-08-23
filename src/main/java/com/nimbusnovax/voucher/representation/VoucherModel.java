package com.nimbusnovax.voucher.representation;

import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/**
 * Mesmo shape de {@link VoucherResponse} (o frontend usa o mesmo tipo TypeScript pra listagem e
 * detalhe) - usado só pelo endpoint de busca paginada, que precisa de {@code PagedModel}. Os
 * demais endpoints (findById/create/update) continuam com {@code VoucherResponse}, sem HATEOAS -
 * ver VoucherModelAssembler para o porquê dessa separação.
 */
@Getter
@Setter
@NoArgsConstructor
@Relation(collectionRelation = "content")
public class VoucherModel extends RepresentationModel<VoucherModel> {

  private UUID id;
  private String code;
  private StatusVoucherEnum status;
  private TypePersonEnum typePerson;
  private String note;
  private LocalDate visitDate;
  private Integer numberOfVisit;
  private BigDecimal totalPrice;
  private BigDecimal advanceValue;
  private BigDecimal totalPriceTickets;
  private BigDecimal totalPriceFoods;
  private Instant confirmationDate;
  private Instant cancellationDate;
  private AgentRef client;
  private AgentRef promoter;
  private AgentRef tourGuide;
  private CancellationReasonRef cancellationReason;
  private List<ItemRef> tickets = List.of();
  private List<ItemRef> foods = List.of();
  private Instant createdAt;
  private Instant updatedAt;

  public record AgentRef(UUID id, String name, String document) {
  }

  public record CancellationReasonRef(UUID id, String name) {
  }

  public record ItemRef(
      UUID id, UUID productId, String productName, Integer quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
  }
}
