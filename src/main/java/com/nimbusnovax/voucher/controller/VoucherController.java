package com.nimbusnovax.voucher.controller;

import com.nimbusnovax.common.security.CheckSecurity;
import com.nimbusnovax.common.web.PageableMapper;
import com.nimbusnovax.common.web.SearchRequest;
import com.nimbusnovax.voucher.core.VoucherService;
import com.nimbusnovax.voucher.dto.request.VoucherRequest;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
import com.nimbusnovax.voucher.model.Voucher;
import com.nimbusnovax.voucher.representation.VoucherModel;
import com.nimbusnovax.voucher.representation.VoucherModelAssembler;
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
@RequestMapping("/bff/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

  private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

  private final VoucherService service;
  private final VoucherModelAssembler modelAssembler;
  private final PagedResourcesAssembler<Voucher> pagedResourcesAssembler;

  @GetMapping("/{id}")
  @CheckSecurity.Voucher.CanConsult
  public VoucherResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  @CheckSecurity.Voucher.CanConsult
  public PagedModel<VoucherModel> search(@RequestBody SearchRequest request) {
    Pageable pageable = PageableMapper.toPageable(request.page(), request.size(), request.sort(), DEFAULT_SORT);
    Page<Voucher> page = service.search(request, pageable);
    return pagedResourcesAssembler.toModel(page, modelAssembler);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @CheckSecurity.Voucher.CanCreate
  public VoucherResponse create(@Valid @RequestBody VoucherRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @CheckSecurity.Voucher.CanChange
  public VoucherResponse update(@PathVariable UUID id, @Valid @RequestBody VoucherRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @CheckSecurity.Voucher.CanDelete
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
