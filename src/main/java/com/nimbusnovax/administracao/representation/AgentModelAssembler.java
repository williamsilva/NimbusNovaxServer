package com.nimbusnovax.administracao.representation;

import com.nimbusnovax.administracao.controller.AgentController;
import com.nimbusnovax.administracao.model.Agent;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

/** Não acessa {@code agentTypes}/{@code addresses}/{@code contacts} (coleções LAZY que a
 *  listagem não exibe - ver {@link AgentModel}), evitando disparar essas queries pra cada agente
 *  da página. */
@Component
public class AgentModelAssembler extends RepresentationModelAssemblerSupport<Agent, AgentModel> {

  public AgentModelAssembler() {
    super(AgentController.class, AgentModel.class);
  }

  @Override
  public AgentModel toModel(Agent agent) {
    AgentModel model = createModelWithId(agent.getId(), agent);

    model.setId(agent.getId());
    model.setCode(agent.getCode());
    model.setName(agent.getName());
    model.setSocialReason(agent.getSocialReason());
    model.setDocument(agent.getDocument());
    model.setRg(agent.getRg());
    model.setSex(agent.getSex());
    model.setTypePerson(agent.getTypePersonEnum());
    model.setCivilState(agent.getCivilStateEnum());
    model.setBirthDate(agent.getBirthDate());
    model.setManager(agent.isManager());
    model.setAttendant(agent.isAttendant());
    model.setStatusClient(agent.getStatusClientEnum());
    model.setStatusProvider(agent.getStatusProviderEnum());
    model.setStatusPromoter(agent.getStatusPromoterEnum());
    model.setStatusEmployee(agent.getStatusEmployeeEnum());
    model.setStatusTourGuide(agent.getStatusTourGuideEnum());
    model.setCreatedAt(agent.getCreatedAt());
    model.setUpdatedAt(agent.getUpdatedAt());

    return model;
  }
}
