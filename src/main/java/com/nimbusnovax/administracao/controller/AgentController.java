package com.nimbusnovax.administracao.controller;

import com.nimbusnovax.administracao.core.AgentService;
import com.nimbusnovax.administracao.dto.request.AgentRequest;
import com.nimbusnovax.administracao.dto.response.AgentOptionResponse;
import com.nimbusnovax.administracao.dto.response.AgentResponse;
import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.administracao.model.enums.TypeAgentEnum;
import com.nimbusnovax.administracao.representation.AgentModel;
import com.nimbusnovax.administracao.representation.AgentModelAssembler;
import com.nimbusnovax.common.security.CheckSecurity;
import com.nimbusnovax.common.web.PageableMapper;
import com.nimbussystems.commons.web.SearchRequest;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/v1/agents")
@RequiredArgsConstructor
public class AgentController {

  private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

  private final AgentService service;
  private final AgentModelAssembler modelAssembler;
  private final PagedResourcesAssembler<Agent> pagedResourcesAssembler;

  @GetMapping("/{id}")
  @CheckSecurity.Agent.CanConsult
  public AgentResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  @CheckSecurity.Agent.CanConsult
  public PagedModel<AgentModel> search(@RequestBody SearchRequest request) {
    Pageable pageable = PageableMapper.toPageable(request.page(), request.size(), request.sort(), DEFAULT_SORT);
    Page<Agent> page = service.search(request, pageable);
    return pagedResourcesAssembler.toModel(page, modelAssembler);
  }

  @GetMapping("/options")
  @CheckSecurity.Agent.CanConsult
  public List<AgentOptionResponse> options(@RequestParam TypeAgentEnum role) {
    return service.findOptions(role);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @CheckSecurity.Agent.CanCreate
  public AgentResponse create(@Valid @RequestBody AgentRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @CheckSecurity.Agent.CanChange
  public AgentResponse update(@PathVariable UUID id, @Valid @RequestBody AgentRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @CheckSecurity.Agent.CanDelete
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
