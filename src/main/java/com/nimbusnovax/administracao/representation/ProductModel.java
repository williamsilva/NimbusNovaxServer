package com.nimbusnovax.administracao.representation;

import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/** Mesmo shape de {@code ProductResponse} - usado só pelo endpoint de busca paginada, ver
 *  {@code com.nimbusnovax.voucher.representation.VoucherModel} para o porquê dessa separação. */
@Getter
@Setter
@NoArgsConstructor
@Relation(collectionRelation = "content")
public class ProductModel extends RepresentationModel<ProductModel> {

  private UUID id;
  private String name;
  private String description;
  private TypeProductEnum typeProduct;
  private BigDecimal amount;
  private LocalDate initialValidate;
  private LocalDate finalValidate;
  private StatusEnum status;
  private Instant createdAt;
  private Instant updatedAt;
}
