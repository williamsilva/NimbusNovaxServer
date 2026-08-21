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
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.web.FilterSupport;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final CityRepository cityRepository;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public AgentResponse findById(UUID id) {
    requireAuthority("AGENTES_CONSULT");
    return toResponse(getOrThrow(id));
  }

  /**
   * Sem paginação/filtro no banco - busca tudo e filtra/ordena/pagina em memória, mesmo padrão de
   * {@code SupplierService.search} (aqui o volume, ~750 linhas, ainda é confortável pra isso; se
   * crescer de forma relevante, trocar por Specification/Pageable do Spring Data sem mudar o
   * contrato do controller).
   */
  @Transactional(readOnly = true)
  public PageResponse<AgentResponse> search(SearchRequest request) {
    requireAuthority("AGENTES_CONSULT");
    List<AgentResponse> all = repository.findAll().stream().map(this::toResponse).toList();
    List<AgentResponse> sorted = sortAgents(filterAgents(all, request), request);

    int page = request.page() == null ? 0 : Math.max(0, request.page());
    int size = request.size() == null || request.size() <= 0 ? 20 : request.size();
    int from = Math.min(page * size, sorted.size());
    int to = Math.min(from + size, sorted.size());

    return PageResponse.of(sorted.subList(from, to), page, size, sorted.size());
  }

  private List<AgentResponse> filterAgents(List<AgentResponse> items, SearchRequest request) {
    Map<String, Object> tableFilters = request.tableFilters();
    Map<String, Object> advanced = request.advanced();

    String code = FilterSupport.textFilter(tableFilters, advanced, "code", "code");
    String name = FilterSupport.textFilter(tableFilters, advanced, "name", "name");
    String document = FilterSupport.textFilter(tableFilters, advanced, "document", "document");
    List<String> typePersonValues = FilterSupport.listFilter(tableFilters, advanced, "typePerson", "typePerson");
    Instant[] createdAtRange = FilterSupport.periodInstantRange(
        tableFilters, advanced, "createdAt", "periodCreatedAt", "createdAt");
    String global = request.globalFilter();

    return items.stream()
        .filter(a -> FilterSupport.containsIgnoreCase(a.code(), code))
        .filter(a -> FilterSupport.containsIgnoreCase(a.name(), name))
        .filter(a -> FilterSupport.containsIgnoreCase(a.document(), document))
        .filter(a -> typePersonValues.isEmpty()
            || (a.typePerson() != null && typePersonValues.contains(a.typePerson().name())))
        .filter(a -> FilterSupport.withinRange(a.createdAt(), createdAtRange))
        .filter(a -> global == null || global.isBlank()
            || FilterSupport.containsIgnoreCase(a.code(), global)
            || FilterSupport.containsIgnoreCase(a.name(), global)
            || FilterSupport.containsIgnoreCase(a.document(), global))
        .toList();
  }

  private List<AgentResponse> sortAgents(List<AgentResponse> items, SearchRequest request) {
    SearchRequest.SortItem sortItem =
        (request.sort() == null || request.sort().isEmpty()) ? null : request.sort().get(0);
    String field = sortItem != null && sortItem.field() != null ? sortItem.field() : "name";
    boolean desc = sortItem != null && sortItem.order() != null && sortItem.order() < 0;

    Comparator<AgentResponse> comparator = switch (field) {
      case "code" -> Comparator.comparing(a -> orEmpty(a.code()), String.CASE_INSENSITIVE_ORDER);
      case "document" -> Comparator.comparing(a -> orEmpty(a.document()), String.CASE_INSENSITIVE_ORDER);
      case "createdAt" -> Comparator.comparing(a -> orEpoch(a.createdAt()));
      default -> Comparator.comparing(a -> orEmpty(a.name()), String.CASE_INSENSITIVE_ORDER);
    };

    if (desc) {
      comparator = comparator.reversed();
    }

    return items.stream().sorted(comparator).toList();
  }

  private static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  private static Instant orEpoch(Instant value) {
    return value == null ? Instant.EPOCH : value;
  }

  /** Item leve para popular selects de outros módulos (ex.: cliente/promotor/guia turístico do
   *  formulário de Voucher) - todos os agentes com o papel pedido, incluindo os inativos nesse
   *  papel (marcados com {@code inactive=true} para o frontend desabilitar a opção, mesmo padrão
   *  de "optionDisabled" já usado no sistema legado). */
  @Transactional(readOnly = true)
  public List<AgentOptionResponse> findOptions(TypeAgentEnum role) {
    requireAuthority("AGENTES_CONSULT");
    return repository.findAll().stream()
        .filter(a -> a.getAgentTypes().stream().anyMatch(t -> t.getTypeAgentEnum() == role))
        .map(a -> new AgentOptionResponse(a.getId(), a.getName(), a.getDocument(), roleStatus(a, role) != StatusEnum.ACTIVE))
        .sorted(Comparator.comparing(AgentOptionResponse::name, String.CASE_INSENSITIVE_ORDER))
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
    requireAuthority("AGENTES_CREATE");
    Agent agent = new Agent();
    agent.setCode(nextCode());
    applyRequest(agent, request);
    UUID userId = currentUserId();
    agent.setCreatedById(userId);
    agent.setUpdatedById(userId);
    return toResponse(save(agent));
  }

  public AgentResponse update(UUID id, AgentRequest request) {
    requireAuthority("AGENTES_CHANGE");
    Agent agent = getOrThrow(id);
    applyRequest(agent, request);
    agent.setUpdatedById(currentUserId());
    return toResponse(save(agent));
  }

  public void delete(UUID id) {
    requireAuthority("AGENTES_DELETE");
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

  /** @param permission nome cru (ex.: "AGENTES_CONSULT") - authorities já vêm prefixadas "PERM_". */
  private void requireAuthority(String permission) {
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
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
