package com.nimbusnovax.administracao.representation;

import com.nimbusnovax.administracao.controller.CancellationReasonController;
import com.nimbusnovax.administracao.model.CancellationReason;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

@Component
public class CancellationReasonModelAssembler
    extends RepresentationModelAssemblerSupport<CancellationReason, CancellationReasonModel> {

  public CancellationReasonModelAssembler() {
    super(CancellationReasonController.class, CancellationReasonModel.class);
  }

  @Override
  public CancellationReasonModel toModel(CancellationReason reason) {
    CancellationReasonModel model = createModelWithId(reason.getId(), reason);

    model.setId(reason.getId());
    model.setName(reason.getName());
    model.setDescription(reason.getDescription());
    model.setStatus(reason.getStatusEnum());
    model.setGeneration(reason.getGenerationEnum());
    model.setCreatedAt(reason.getCreatedAt());
    model.setUpdatedAt(reason.getUpdatedAt());

    return model;
  }
}
