package com.nimbusnovax.administracao.controller;

import com.nimbusnovax.administracao.core.ProductService;
import com.nimbusnovax.administracao.dto.request.ProductRequest;
import com.nimbusnovax.administracao.dto.response.ProductResponse;
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
@RequestMapping("/bff/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService service;

  @GetMapping("/{id}")
  public ProductResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  public PageResponse<ProductResponse> search(@RequestBody SearchRequest request) {
    return service.search(request);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductResponse create(@Valid @RequestBody ProductRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }

  @PostMapping("/{id}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activate(@PathVariable UUID id) {
    service.activate(id);
  }

  @PostMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable UUID id) {
    service.deactivate(id);
  }
}
