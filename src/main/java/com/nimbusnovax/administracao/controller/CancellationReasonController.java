package com.nimbusnovax.administracao.controller;

import com.nimbusnovax.administracao.core.CancellationReasonService;
import com.nimbusnovax.administracao.dto.request.CancellationReasonRequest;
import com.nimbusnovax.administracao.dto.response.CancellationReasonResponse;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/v1/cancellation-reasons")
@RequiredArgsConstructor
public class CancellationReasonController {

  private final CancellationReasonService service;

  @GetMapping("/{id}")
  public CancellationReasonResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  public PageResponse<CancellationReasonResponse> search(@RequestBody SearchRequest request) {
    return service.search(request);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CancellationReasonResponse create(@Valid @RequestBody CancellationReasonRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public CancellationReasonResponse update(
      @PathVariable UUID id, @Valid @RequestBody CancellationReasonRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
