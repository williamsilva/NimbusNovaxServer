package com.nimbusnovax.administracao.controller;

import com.nimbusnovax.administracao.core.CancellationReasonService;
import com.nimbusnovax.administracao.dto.request.CancellationReasonRequest;
import com.nimbusnovax.administracao.dto.response.CancellationReasonResponse;
import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.representation.CancellationReasonModel;
import com.nimbusnovax.administracao.representation.CancellationReasonModelAssembler;
import com.nimbusnovax.common.security.CheckSecurity;
import com.nimbusnovax.common.web.PageableMapper;
import com.nimbussystems.commons.web.SearchRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/v1/cancellation-reasons")
@RequiredArgsConstructor
public class CancellationReasonController {

  private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

  private final CancellationReasonService service;
  private final CancellationReasonModelAssembler modelAssembler;
  private final PagedResourcesAssembler<CancellationReason> pagedResourcesAssembler;

  @GetMapping("/{id}")
  @CheckSecurity.CancellationReason.CanConsult
  public CancellationReasonResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  @CheckSecurity.CancellationReason.CanConsult
  public PagedModel<CancellationReasonModel> search(@RequestBody SearchRequest request) {
    Pageable pageable = PageableMapper.toPageable(request.page(), request.size(), request.sort(), DEFAULT_SORT);
    Page<CancellationReason> page = service.search(request, pageable);
    return pagedResourcesAssembler.toModel(page, modelAssembler);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @CheckSecurity.CancellationReason.CanCreate
  public CancellationReasonResponse create(@Valid @RequestBody CancellationReasonRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @CheckSecurity.CancellationReason.CanChange
  public CancellationReasonResponse update(
      @PathVariable UUID id, @Valid @RequestBody CancellationReasonRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @CheckSecurity.CancellationReason.CanDelete
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
