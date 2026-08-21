package com.nimbusnovax.administracao.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nimbusnovax.administracao.model.enums.TypeAgentEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Papel (Client/Provider/Promoter/Employee/TourGuide) que um {@code Agent} assume - um agente
 *  pode ter vários, um por linha. */
@Getter
@Setter
@Entity
@Table(name = "agent_types")
public class AgentType {

  @Id
  @GeneratedValue
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "agent_id", nullable = false)
  @JsonIgnore
  private Agent agent;

  @Column(name = "type_agent", nullable = false)
  private Integer typeAgent;

  /** Código 0 é lixo residual de dados legados (algumas linhas migradas do Novax antigo com
   *  type_agent=0/"NULL") - tratado como ausência de papel, mesmo padrão de código-0-vira-null
   *  usado pelos campos opcionais de {@link Agent}. */
  public TypeAgentEnum getTypeAgentEnum() {
    return typeAgent == null || typeAgent == 0 ? null : TypeAgentEnum.fromCode(typeAgent);
  }

  public void setTypeAgentEnum(TypeAgentEnum typeAgent) {
    this.typeAgent = TypeAgentEnum.toCode(typeAgent);
  }
}
