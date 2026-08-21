package com.nimbusnovax.administracao.controller;

import com.nimbusnovax.administracao.core.AgentService;
import com.nimbusnovax.administracao.dto.request.AgentRequest;
import com.nimbusnovax.administracao.dto.response.AgentOptionResponse;
import com.nimbusnovax.administracao.dto.response.AgentResponse;
import com.nimbusnovax.administracao.model.enums.TypeAgentEnum;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

  private final AgentService service;

  @GetMapping("/{id}")
  public AgentResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  public PageResponse<AgentResponse> search(@RequestBody SearchRequest request) {
    return service.search(request);
  }

  @GetMapping("/options")
  public List<AgentOptionResponse> options(@RequestParam TypeAgentEnum role) {
    return service.findOptions(role);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AgentResponse create(@Valid @RequestBody AgentRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public AgentResponse update(@PathVariable UUID id, @Valid @RequestBody AgentRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
