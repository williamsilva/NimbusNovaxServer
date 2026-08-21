package com.nimbusnovax.voucher.controller;

import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import com.nimbusnovax.voucher.core.VoucherService;
import com.nimbusnovax.voucher.dto.request.VoucherRequest;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
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
@RequestMapping("/bff/v1/vouchers")
@RequiredArgsConstructor
public class VoucherController {

  private final VoucherService service;

  @GetMapping("/{id}")
  public VoucherResponse findById(@PathVariable UUID id) {
    return service.findById(id);
  }

  @PostMapping("/search")
  public PageResponse<VoucherResponse> search(@RequestBody SearchRequest request) {
    return service.search(request);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public VoucherResponse create(@Valid @RequestBody VoucherRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public VoucherResponse update(@PathVariable UUID id, @Valid @RequestBody VoucherRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) {
    service.delete(id);
  }
}
