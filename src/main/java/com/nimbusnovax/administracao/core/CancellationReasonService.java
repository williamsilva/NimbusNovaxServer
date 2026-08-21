package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.dto.request.CancellationReasonRequest;
import com.nimbusnovax.administracao.dto.response.CancellationReasonResponse;
import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.model.enums.GenerationEnum;
import com.nimbusnovax.administracao.repository.CancellationReasonRepository;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.web.FilterSupport;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regras replicadas do {@code CancellationReasonService} do sistema legado: nome único;
 * {@code generation} sempre forçado para USER no save, mesmo pra um motivo que já era SYSTEM (o
 * bloqueio de edição/exclusão dos motivos SYSTEM existe só na UI, nunca existiu aqui no backend -
 * fidelidade total ao antigo, não uma regra nova).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CancellationReasonService {

  private final CancellationReasonRepository repository;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public CancellationReasonResponse findById(UUID id) {
    requireAuthority("MOTIVO_CANCELAMENTO_CONSULT");
    return toResponse(getOrThrow(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<CancellationReasonResponse> search(SearchRequest request) {
    requireAuthority("MOTIVO_CANCELAMENTO_CONSULT");
    List<CancellationReasonResponse> all = repository.findAll().stream().map(this::toResponse).toList();
    List<CancellationReasonResponse> sorted = sortReasons(filterReasons(all, request), request);

    int page = request.page() == null ? 0 : Math.max(0, request.page());
    int size = request.size() == null || request.size() <= 0 ? 20 : request.size();
    int from = Math.min(page * size, sorted.size());
    int to = Math.min(from + size, sorted.size());

    return PageResponse.of(sorted.subList(from, to), page, size, sorted.size());
  }

  private List<CancellationReasonResponse> filterReasons(List<CancellationReasonResponse> items, SearchRequest request) {
    Map<String, Object> tableFilters = request.tableFilters();
    Map<String, Object> advanced = request.advanced();

    String name = FilterSupport.textFilter(tableFilters, advanced, "name", "name");
    List<String> statusValues = FilterSupport.listFilter(tableFilters, advanced, "status", "status");
    String global = request.globalFilter();

    return items.stream()
        .filter(r -> FilterSupport.containsIgnoreCase(r.name(), name))
        .filter(r -> statusValues.isEmpty() || (r.status() != null && statusValues.contains(r.status().name())))
        .filter(r -> global == null || global.isBlank() || FilterSupport.containsIgnoreCase(r.name(), global))
        .toList();
  }

  private List<CancellationReasonResponse> sortReasons(List<CancellationReasonResponse> items, SearchRequest request) {
    SearchRequest.SortItem sortItem =
        (request.sort() == null || request.sort().isEmpty()) ? null : request.sort().get(0);
    String field = sortItem != null && sortItem.field() != null ? sortItem.field() : "name";
    boolean desc = sortItem != null && sortItem.order() != null && sortItem.order() < 0;

    Comparator<CancellationReasonResponse> comparator = switch (field) {
      case "createdAt" -> Comparator.comparing(r -> orEpoch(r.createdAt()));
      default -> Comparator.comparing(r -> orEmpty(r.name()), String.CASE_INSENSITIVE_ORDER);
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

  public CancellationReasonResponse create(CancellationReasonRequest request) {
    requireAuthority("MOTIVO_CANCELAMENTO_CREATE");
    CancellationReason reason = new CancellationReason();
    applyRequest(reason, request);
    UUID userId = currentUserId();
    reason.setCreatedById(userId);
    reason.setUpdatedById(userId);
    return toResponse(save(reason));
  }

  public CancellationReasonResponse update(UUID id, CancellationReasonRequest request) {
    requireAuthority("MOTIVO_CANCELAMENTO_CHANGE");
    CancellationReason reason = getOrThrow(id);
    applyRequest(reason, request);
    reason.setUpdatedById(currentUserId());
    return toResponse(save(reason));
  }

  public void delete(UUID id) {
    requireAuthority("MOTIVO_CANCELAMENTO_DELETE");
    CancellationReason reason = getOrThrow(id);
    try {
      repository.delete(reason);
      repository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete a reason that has links");
    }
  }

  private CancellationReason save(CancellationReason reason) {
    repository.findByName(reason.getName()).ifPresent(existing -> {
      if (reason.getId() == null || !existing.getId().equals(reason.getId())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "There is already a reason registered with the name " + reason.getName());
      }
    });

    reason.setGenerationEnum(GenerationEnum.USER);
    return repository.save(reason);
  }

  private UUID currentUserId() {
    try {
      return UUID.fromString(currentUserProvider.requireUserId());
    } catch (IllegalStateException | IllegalArgumentException e) {
      return null;
    }
  }

  private void requireAuthority(String permission) {
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
    }
  }

  private CancellationReason getOrThrow(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cancellation reason not found: " + id));
  }

  private void applyRequest(CancellationReason reason, CancellationReasonRequest request) {
    reason.setName(request.name());
    reason.setDescription(request.description());
    if (request.status() != null) {
      reason.setStatusEnum(request.status());
    }
  }

  private CancellationReasonResponse toResponse(CancellationReason reason) {
    return new CancellationReasonResponse(
        reason.getId(),
        reason.getName(),
        reason.getDescription(),
        reason.getStatusEnum(),
        reason.getGenerationEnum(),
        reason.getCreatedAt(),
        reason.getUpdatedAt());
  }
}
