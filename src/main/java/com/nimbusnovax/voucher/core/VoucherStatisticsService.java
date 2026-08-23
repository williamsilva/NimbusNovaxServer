package com.nimbusnovax.voucher.core;

import com.nimbusnovax.voucher.dto.response.VoucherStatisticsResponse.ByStatus;
import com.nimbusnovax.voucher.dto.response.VoucherStatisticsResponse.Totals;
import com.nimbusnovax.voucher.dto.response.VoucherStatisticsResponse.TopClient;
import com.nimbusnovax.voucher.model.Voucher;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import com.nimbusnovax.voucher.repository.VoucherRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estatísticas do dashboard de voucher, mesmas 3 consultas do {@code dashboard-voucher} do
 * sistema legado (Novax antigo): distribuição por status, totais (nº de clientes distintos/
 * vouchers/valores) e top clientes por valor total - filtradas por período opcional (data de
 * criação do voucher, ver {@code VoucherDTOFilter} no legado). Volume atual (~1000 vouchers)
 * comporta processar em memória, mesmo critério já usado em {@link VoucherService#search}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoucherStatisticsService {

  private static final int TOP_CLIENTS_LIMIT = 10;

  private final VoucherRepository repository;

  public List<ByStatus> byStatus(LocalDate firstPeriod, LocalDate finalPeriod) {
    Map<StatusVoucherEnum, Long> byStatus = filtered(firstPeriod, finalPeriod).stream()
        .collect(Collectors.groupingBy(Voucher::getStatusEnum, Collectors.counting()));

    return byStatus.entrySet().stream()
        .map(e -> new ByStatus(e.getKey(), e.getValue()))
        .sorted(Comparator.comparing(b -> b.status().getCode()))
        .toList();
  }

  public Totals totals(LocalDate firstPeriod, LocalDate finalPeriod) {
    List<Voucher> vouchers = filtered(firstPeriod, finalPeriod);

    long clientCount = vouchers.stream().map(v -> v.getClient().getId()).distinct().count();
    BigDecimal totalPrice = vouchers.stream().map(Voucher::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPriceTickets = vouchers.stream()
        .map(Voucher::getTotalPriceTickets).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalPriceFoods = vouchers.stream()
        .map(Voucher::getTotalPriceFoods).reduce(BigDecimal.ZERO, BigDecimal::add);

    return new Totals(clientCount, vouchers.size(), totalPrice, totalPriceTickets, totalPriceFoods);
  }

  public List<TopClient> topClients(LocalDate firstPeriod, LocalDate finalPeriod) {
    List<Voucher> vouchers = filtered(firstPeriod, finalPeriod);

    Map<UUID, List<Voucher>> byClient = vouchers.stream()
        .collect(Collectors.groupingBy(v -> v.getClient().getId()));

    return byClient.values().stream()
        .map(list -> new TopClient(
            list.get(0).getClient().getId(),
            list.get(0).getClient().getName(),
            list.size(),
            list.stream().map(Voucher::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add)))
        .sorted(Comparator.comparing(TopClient::totalPrice).reversed())
        .limit(TOP_CLIENTS_LIMIT)
        .toList();
  }

  private List<Voucher> filtered(LocalDate firstPeriod, LocalDate finalPeriod) {
    return repository.findAll().stream()
        .filter(v -> withinPeriod(v, firstPeriod, finalPeriod))
        .toList();
  }

  private boolean withinPeriod(Voucher voucher, LocalDate firstPeriod, LocalDate finalPeriod) {
    if (firstPeriod == null && finalPeriod == null) {
      return true;
    }
    if (voucher.getCreatedAt() == null) {
      return false;
    }
    LocalDate createdDate = voucher.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
    if (firstPeriod != null && createdDate.isBefore(firstPeriod)) {
      return false;
    }
    return finalPeriod == null || !createdDate.isAfter(finalPeriod);
  }
}
