package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.dto.request.CancellationReasonRequest;
import com.nimbusnovax.administracao.dto.response.CancellationReasonResponse;
import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.model.enums.GenerationEnum;
import com.nimbusnovax.administracao.repository.CancellationReasonRepository;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.web.SearchRequest;
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
  private final CancellationReasonSpecs cancellationReasonSpecs;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public CancellationReasonResponse findById(UUID id) {
    return toResponse(getOrThrow(id));
  }

  /** Ver {@link com.nimbusnovax.voucher.core.VoucherSpecs} para o padrão. */
  @Transactional(readOnly = true)
  public Page<CancellationReason> search(SearchRequest request, Pageable pageable) {
    Specification<CancellationReason> spec = cancellationReasonSpecs.fromRequest(request);
    return repository.findAll(spec, pageable);
  }

  public CancellationReasonResponse create(CancellationReasonRequest request) {
    CancellationReason reason = new CancellationReason();
    applyRequest(reason, request);
    UUID userId = currentUserId();
    reason.setCreatedById(userId);
    reason.setUpdatedById(userId);
    return toResponse(save(reason));
  }

  public CancellationReasonResponse update(UUID id, CancellationReasonRequest request) {
    CancellationReason reason = getOrThrow(id);
    applyRequest(reason, request);
    reason.setUpdatedById(currentUserId());
    return toResponse(save(reason));
  }

  public void delete(UUID id) {
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
