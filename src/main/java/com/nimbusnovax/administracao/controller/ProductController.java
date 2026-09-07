package com.nimbusnovax.administracao.controller;

import com.nimbusnovax.administracao.core.ProductService;
import com.nimbusnovax.administracao.dto.request.ProductRequest;
import com.nimbusnovax.administracao.dto.response.ProductOptionResponse;
import com.nimbusnovax.administracao.dto.response.ProductResponse;
import com.nimbusnovax.administracao.model.Product;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import com.nimbusnovax.administracao.representation.ProductModel;
import com.nimbusnovax.administracao.representation.ProductModelAssembler;
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
@RequestMapping("/bff/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "name");

  private final ProductService service;
  private final ProductModelAssembler modelAssembler;
  private final PagedResourcesAssembler<Product> pagedResourcesAssembler;

  @GetMapping("/{id}")
  @CheckSecurity.Product.CanConsult
  public ProductResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  @CheckSecurity.Product.CanConsult
  public PagedModel<ProductModel> search(@RequestBody SearchRequest request) {
    Pageable pageable = PageableMapper.toPageable(request.page(), request.size(), request.sort(), DEFAULT_SORT);
    Page<Product> page = service.search(request, pageable);
    return pagedResourcesAssembler.toModel(page, modelAssembler);
  }

  @GetMapping("/options")
  @CheckSecurity.Product.CanConsult
  public List<ProductOptionResponse> options(@RequestParam TypeProductEnum typeProduct) {
    return service.findOptions(typeProduct);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @CheckSecurity.Product.CanCreate
  public ProductResponse create(@Valid @RequestBody ProductRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @CheckSecurity.Product.CanChange
  public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @CheckSecurity.Product.CanDelete
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }

  @PostMapping("/{id}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @CheckSecurity.Product.CanChange
  public void activate(@PathVariable UUID id) {
    service.activate(id);
  }

  @PostMapping("/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @CheckSecurity.Product.CanChange
  public void deactivate(@PathVariable UUID id) {
    service.deactivate(id);
  }
}
