package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.dto.request.AgentRequest;
import com.nimbusnovax.administracao.dto.response.AgentOptionResponse;
import com.nimbusnovax.administracao.dto.response.AgentResponse;
import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.administracao.model.AgentAddress;
import com.nimbusnovax.administracao.model.AgentContact;
import com.nimbusnovax.administracao.model.AgentType;
import com.nimbusnovax.administracao.model.City;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeAgentEnum;
import com.nimbusnovax.administracao.repository.AgentRepository;
import com.nimbusnovax.administracao.repository.CityRepository;
import com.nimbussystems.commons.security.CurrentUserProvider;
import com.nimbussystems.commons.web.SearchRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regras de negócio replicadas do {@code AgentService} do sistema legado (Novax antigo): nome,
 * código e documento únicos; código gerado pelo servidor ("1000+", nunca escolhido pelo usuário);
 * papéis/endereços/contatos são substituídos por inteiro a cada save (clear + re-add, não merge
 * item a item - ver {@link Agent}).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

  private final AgentRepository repository;
  private final AgentSpecs agentSpecs;
  private final CityRepository cityRepository;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public AgentResponse findById(UUID id) {
    return toResponse(getOrThrow(id));
  }

  /** Filtro/ordenação/paginação reais no banco via {@link AgentSpecs} (Specification) - ver
   *  {@link com.nimbusnovax.voucher.core.VoucherSpecs} para o padrão. */
  @Transactional(readOnly = true)
  public Page<Agent> search(SearchRequest request, Pageable pageable) {
    Specification<Agent> spec = agentSpecs.fromRequest(request);
    return repository.findAll(spec, pageable);
  }

  /** Item leve para popular selects de outros módulos (ex.: cliente/promotor/guia turístico do
   *  formulário de Voucher) - todos os agentes com o papel pedido, incluindo os inativos nesse
   *  papel (marcados com {@code inactive=true} para o frontend desabilitar a opção, mesmo padrão
   *  de "optionDisabled" já usado no sistema legado). Ordenados com os ativos primeiro, depois
   *  por nome - facilita achar quem de fato pode ser escolhido antes dos inativos. */
  @Transactional(readOnly = true)
  public List<AgentOptionResponse> findOptions(TypeAgentEnum role) {
    return repository.findAll().stream()
        .filter(a -> a.getAgentTypes().stream().anyMatch(t -> t.getTypeAgentEnum() == role))
        .map(a -> new AgentOptionResponse(a.getId(), a.getName(), a.getDocument(), roleStatus(a, role) != StatusEnum.ACTIVE))
        .sorted(Comparator.comparing(AgentOptionResponse::inactive)
            .thenComparing(AgentOptionResponse::name, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private StatusEnum roleStatus(Agent agent, TypeAgentEnum role) {
    return switch (role) {
      case CLIENT -> agent.getStatusClientEnum();
      case PROVIDER -> agent.getStatusProviderEnum();
      case PROMOTER -> agent.getStatusPromoterEnum();
      case EMPLOYEE -> agent.getStatusEmployeeEnum();
      case TOUR_GUIDE -> agent.getStatusTourGuideEnum();
    };
  }

  public AgentResponse create(AgentRequest request) {
    Agent agent = new Agent();
    agent.setCode(nextCode());
    applyRequest(agent, request);
    UUID userId = currentUserId();
    agent.setCreatedById(userId);
    agent.setUpdatedById(userId);
    return toResponse(save(agent));
  }

  public AgentResponse update(UUID id, AgentRequest request) {
    Agent agent = getOrThrow(id);
    applyRequest(agent, request);
    agent.setUpdatedById(currentUserId());
    return toResponse(save(agent));
  }

  public void delete(UUID id) {
    Agent agent = getOrThrow(id);
    try {
      repository.delete(agent);
      repository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete an agent that has links");
    }
  }

  private Agent save(Agent agent) {
    validateNameAvailability(agent);
    validateDocumentAvailability(agent);
    return repository.save(agent);
  }

  private String nextCode() {
    Integer max = repository.findMaxNumericCode();
    long next = max == null ? 1000L : (long) max + 1;
    return String.valueOf(next);
  }

  private void validateNameAvailability(Agent agent) {
    repository.findByName(agent.getName()).ifPresent(existing -> {
      if (agent.getId() == null || !existing.getId().equals(agent.getId())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "An agent with name already exists: " + agent.getName());
      }
    });
  }

  private void validateDocumentAvailability(Agent agent) {
    repository.findByDocument(agent.getDocument()).ifPresent(existing -> {
      if (agent.getId() == null || !existing.getId().equals(agent.getId())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "There is already an agent with document " + agent.getDocument());
      }
    });
  }

  private UUID currentUserId() {
    try {
      return UUID.fromString(currentUserProvider.requireUserId());
    } catch (IllegalStateException | IllegalArgumentException e) {
      return null;
    }
  }

  private Agent getOrThrow(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found: " + id));
  }

  private void applyRequest(Agent agent, AgentRequest request) {
    agent.setName(request.name());
    agent.setSocialReason(request.socialReason());
    agent.setDocument(request.document());
    agent.setRg(request.rg());
    agent.setSex(request.sex());
    agent.setTypePersonEnum(request.typePerson());
    agent.setCivilStateEnum(request.civilState());
    agent.setBirthDate(request.birthDate());
    agent.setManager(request.isManager());
    agent.setAttendant(request.isAttendant());
    agent.setStatusClientEnum(request.statusClient());
    agent.setStatusProviderEnum(request.statusProvider());
    agent.setStatusPromoterEnum(request.statusPromoter());
    agent.setStatusEmployeeEnum(request.statusEmployee());
    agent.setStatusTourGuideEnum(request.statusTourGuide());

    agent.getAgentTypes().clear();
    for (TypeAgentEnum role : request.roles() == null ? List.<TypeAgentEnum>of() : request.roles()) {
      AgentType type = new AgentType();
      type.setAgent(agent);
      type.setTypeAgentEnum(role);
      agent.getAgentTypes().add(type);
    }

    agent.getAddresses().clear();
    List<AgentRequest.AddressRequest> addresses = request.addresses() == null ? List.of() : request.addresses();
    for (AgentRequest.AddressRequest addressRequest : addresses) {
      AgentAddress address = new AgentAddress();
      address.setAgent(agent);
      address.setStreet(addressRequest.street());
      address.setNumber(addressRequest.number());
      address.setComplement(addressRequest.complement());
      address.setBurgh(addressRequest.burgh());
      address.setPostalCode(addressRequest.postalCode());
      if (addressRequest.cityId() != null) {
        City city = cityRepository.findById(addressRequest.cityId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "City not found: " + addressRequest.cityId()));
        address.setCity(city);
      }
      agent.getAddresses().add(address);
    }

    agent.getContacts().clear();
    List<AgentRequest.ContactRequest> contacts = request.contacts() == null ? List.of() : request.contacts();
    for (AgentRequest.ContactRequest contactRequest : contacts) {
      AgentContact contact = new AgentContact();
      contact.setAgent(agent);
      contact.setName(contactRequest.name());
      contact.setCellphone(contactRequest.cellphone());
      contact.setTelephone(contactRequest.telephone());
      contact.setEmail(contactRequest.email());
      agent.getContacts().add(contact);
    }
  }

  private AgentResponse toResponse(Agent agent) {
    List<TypeAgentEnum> roles = agent.getAgentTypes().stream()
        .map(AgentType::getTypeAgentEnum)
        .filter(Objects::nonNull)
        .toList();

    List<AgentResponse.AddressResponse> addresses = agent.getAddresses().stream()
        .map(a -> new AgentResponse.AddressResponse(
            a.getId(), a.getStreet(), a.getNumber(), a.getComplement(), a.getBurgh(), a.getPostalCode(),
            a.getCity() != null ? a.getCity().getId() : null,
            a.getCity() != null ? a.getCity().getName() : null,
            a.getCity() != null ? a.getCity().getState().getUf() : null))
        .toList();

    List<AgentResponse.ContactResponse> contacts = agent.getContacts().stream()
        .map(c -> new AgentResponse.ContactResponse(
            c.getId(), c.getName(), c.getCellphone(), c.getTelephone(), c.getEmail()))
        .toList();

    return new AgentResponse(
        agent.getId(),
        agent.getCode(),
        agent.getName(),
        agent.getSocialReason(),
        agent.getDocument(),
        agent.getRg(),
        agent.getSex(),
        agent.getTypePersonEnum(),
        agent.getCivilStateEnum(),
        agent.getBirthDate(),
        agent.isManager(),
        agent.isAttendant(),
        roles,
        agent.getStatusClientEnum(),
        agent.getStatusProviderEnum(),
        agent.getStatusPromoterEnum(),
        agent.getStatusEmployeeEnum(),
        agent.getStatusTourGuideEnum(),
        addresses,
        contacts,
        agent.getCreatedAt(),
        agent.getUpdatedAt());
  }
}
