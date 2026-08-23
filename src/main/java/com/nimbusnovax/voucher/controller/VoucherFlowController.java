package com.nimbusnovax.voucher.controller;

import com.nimbusnovax.common.security.CheckSecurity;
import com.nimbusnovax.voucher.core.VoucherFlowService;
import com.nimbusnovax.voucher.dto.request.VoucherCancellationRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/bff/v1/vouchers/{id}")
@RequiredArgsConstructor
public class VoucherFlowController {

  private final VoucherFlowService flowService;

  @PutMapping("/confirm")
  @CheckSecurity.Voucher.CanChange
  public void confirm(@PathVariable UUID id) {
    flowService.confirm(id);
  }

  @PutMapping("/not-confirm")
  @CheckSecurity.Voucher.CanChange
  public void notConfirm(@PathVariable UUID id) {
    flowService.notConfirm(id);
  }

  @PutMapping("/change")
  @CheckSecurity.Voucher.CanChange
  public void change(@PathVariable UUID id) {
    flowService.change(id);
  }

  @PutMapping("/cancel")
  @CheckSecurity.Voucher.CanChange
  public void cancel(@PathVariable UUID id, @Valid @RequestBody VoucherCancellationRequest request) {
    flowService.cancel(id, request.cancellationReasonId());
  }

  @PutMapping("/send-email")
  @CheckSecurity.Voucher.CanChange
  public void sendEmail(@PathVariable UUID id) {
    flowService.sendVoucherEmail(id);
  }

  @GetMapping("/to-view")
  @CheckSecurity.Authenticated
  public ResponseEntity<byte[]> toView(@PathVariable UUID id) {
    byte[] pdf = flowService.renderPdf(id);
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"voucher-" + id + ".pdf\"");
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).headers(headers).body(pdf);
  }
}
