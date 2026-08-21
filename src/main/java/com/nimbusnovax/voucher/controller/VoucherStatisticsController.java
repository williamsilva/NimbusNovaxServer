package com.nimbusnovax.voucher.controller;

import com.nimbusnovax.voucher.core.VoucherStatisticsService;
import com.nimbusnovax.voucher.dto.response.VoucherStatisticsResponse.ByStatus;
import com.nimbusnovax.voucher.dto.response.VoucherStatisticsResponse.TopClient;
import com.nimbusnovax.voucher.dto.response.VoucherStatisticsResponse.Totals;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bff/v1/vouchers/statistics")
@RequiredArgsConstructor
public class VoucherStatisticsController {

  private final VoucherStatisticsService service;

  @GetMapping("/by-status")
  public List<ByStatus> byStatus(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate firstPeriod,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate finalPeriod) {
    return service.byStatus(firstPeriod, finalPeriod);
  }

  @GetMapping("/totals")
  public Totals totals(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate firstPeriod,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate finalPeriod) {
    return service.totals(firstPeriod, finalPeriod);
  }

  @GetMapping("/top-clients")
  public List<TopClient> topClients(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate firstPeriod,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate finalPeriod) {
    return service.topClients(firstPeriod, finalPeriod);
  }
}
