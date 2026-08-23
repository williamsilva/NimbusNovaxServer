package com.nimbusnovax.administracao.representation;

import com.nimbusnovax.administracao.model.enums.CivilStateEnum;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeAgentEnum;
import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

/** Mesmo shape de {@code AgentResponse} - usado só pelo endpoint de busca paginada, ver
 *  {@code com.nimbusnovax.voucher.representation.VoucherModel} para o porquê dessa separação.
 *  {@code addresses}/{@code contacts} sempre vazios aqui (ver {@link AgentModelAssembler}) - a
 *  listagem não os exibe (só {@code typePerson}), e populá-los dispararia a cadeia lazy
 *  agentTypes/addresses→city→state/contacts por linha (N+1). */
@Getter
@Setter
@NoArgsConstructor
@Relation(collectionRelation = "content")
public class AgentModel extends RepresentationModel<AgentModel> {

  private UUID id;
  private String code;
  private String name;
  private String socialReason;
  private String document;
  private String rg;
  private String sex;
  private TypePersonEnum typePerson;
  private CivilStateEnum civilState;
  private LocalDate birthDate;
  private boolean manager;
  private boolean attendant;
  private List<TypeAgentEnum> roles = List.of();
  private StatusEnum statusClient;
  private StatusEnum statusProvider;
  private StatusEnum statusPromoter;
  private StatusEnum statusEmployee;
  private StatusEnum statusTourGuide;
  private List<AddressRef> addresses = List.of();
  private List<ContactRef> contacts = List.of();
  private Instant createdAt;
  private Instant updatedAt;

  public record AddressRef(
      UUID id, String street, String number, String complement, String burgh, String postalCode,
      UUID cityId, String cityName, String stateUf) {
  }

  public record ContactRef(UUID id, String name, String cellphone, String telephone, String email) {
  }
}
