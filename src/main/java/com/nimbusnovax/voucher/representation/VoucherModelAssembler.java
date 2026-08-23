package com.nimbusnovax.voucher.representation;

import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.voucher.controller.VoucherController;
import com.nimbusnovax.voucher.model.Voucher;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

/** Mapeia {@link Voucher} (entity) para {@link VoucherModel} - usado só pela listagem paginada
 *  (ver VoucherController.search), por isso nunca acessa {@code tickets}/{@code foods} (coleções
 *  LAZY que a tabela não exibe - ver VoucherSpecs para o porquê a query já traz as demais
 *  associações via fetch join). */
@Component
public class VoucherModelAssembler extends RepresentationModelAssemblerSupport<Voucher, VoucherModel> {

  public VoucherModelAssembler() {
    super(VoucherController.class, VoucherModel.class);
  }

  @Override
  public VoucherModel toModel(Voucher voucher) {
    VoucherModel model = createModelWithId(voucher.getId(), voucher);

    model.setId(voucher.getId());
    model.setCode(voucher.getCode());
    model.setStatus(voucher.getStatusEnum());
    model.setTypePerson(voucher.getTypePersonEnum());
    model.setNote(voucher.getNote());
    model.setVisitDate(voucher.getVisitDate());
    model.setNumberOfVisit(voucher.getNumberOfVisit());
    model.setTotalPrice(voucher.getTotalPrice());
    model.setAdvanceValue(voucher.getAdvanceValue());
    model.setTotalPriceTickets(voucher.getTotalPriceTickets());
    model.setTotalPriceFoods(voucher.getTotalPriceFoods());
    model.setConfirmationDate(voucher.getConfirmationDate());
    model.setCancellationDate(voucher.getCancellationDate());
    model.setClient(toAgentRef(voucher.getClient()));
    model.setPromoter(toAgentRef(voucher.getPromoter()));
    model.setTourGuide(toAgentRef(voucher.getTourGuide()));
    model.setCancellationReason(voucher.getCancellationReason() == null ? null
        : new VoucherModel.CancellationReasonRef(
            voucher.getCancellationReason().getId(), voucher.getCancellationReason().getName()));
    model.setCreatedAt(voucher.getCreatedAt());
    model.setUpdatedAt(voucher.getUpdatedAt());

    return model;
  }

  private VoucherModel.AgentRef toAgentRef(Agent agent) {
    return agent == null ? null : new VoucherModel.AgentRef(agent.getId(), agent.getName(), agent.getDocument());
  }
}
