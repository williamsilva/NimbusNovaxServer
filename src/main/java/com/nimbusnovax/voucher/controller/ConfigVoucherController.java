package com.nimbusnovax.voucher.controller;

import com.nimbusnovax.voucher.core.ConfigVoucherService;
import com.nimbusnovax.voucher.dto.request.ConfigVoucherRequest;
import com.nimbusnovax.voucher.dto.response.ConfigVoucherResponse;
import com.nimbusnovax.voucher.dto.response.VoucherNotificationRecipientResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/v1/voucher-config")
@RequiredArgsConstructor
public class ConfigVoucherController {

  private final ConfigVoucherService service;

  @GetMapping
  public ConfigVoucherResponse find() {
    return service.find();
  }

  @PutMapping
  public ConfigVoucherResponse update(@Valid @RequestBody ConfigVoucherRequest request) {
    return service.update(request);
  }

  @GetMapping("/notification-recipients")
  public List<VoucherNotificationRecipientResponse> notificationRecipients() {
    return service.notificationRecipients();
  }
}
