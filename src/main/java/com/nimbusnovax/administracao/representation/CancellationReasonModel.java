package com.nimbusnovax.administracao.representation;

import com.nimbusnovax.administracao.model.enums.GenerationEnum;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/** Mesmo shape de {@code CancellationReasonResponse} - ver
 *  {@code com.nimbusnovax.voucher.representation.VoucherModel} para o porquê dessa separação. */
@Getter
@Setter
@NoArgsConstructor
@Relation(collectionRelation = "content")
public class CancellationReasonModel extends RepresentationModel<CancellationReasonModel> {

  private UUID id;
  private String name;
  private String description;
  private StatusEnum status;
  private GenerationEnum generation;
  private Instant createdAt;
  private Instant updatedAt;
}
