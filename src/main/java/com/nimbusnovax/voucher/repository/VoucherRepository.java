package com.nimbusnovax.voucher.repository;

import com.nimbusnovax.voucher.model.Voucher;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

  Optional<Voucher> findByCode(String code);

  List<Voucher> findByStatus(Integer status);

  /** Maior sufixo numérico já usado no código do voucher (ex.: "DWP1926" -> 1926) - base do
   *  próximo código gerado (ver VoucherService.generateVoucherCode). */
  @Query(value = "SELECT MAX(CAST(substring(code, '[0-9]+$') AS integer)) FROM vouchers", nativeQuery = true)
  Integer findMaxNumericSuffix();

  @Query("SELECT COUNT(v) FROM Voucher v WHERE v.client.id = :clientId AND v.status = :status")
  long countByClientAndStatus(@Param("clientId") UUID clientId, @Param("status") Integer status);

  /** Mesmo critério de {@link #countByClientAndStatus}, excluindo um voucher específico da
   *  contagem - usado ao editar um voucher já DEALING (ver VoucherService.update), pra não se
   *  autobloquear ao trocar o cliente de um voucher que já está negociando. */
  @Query("SELECT COUNT(v) FROM Voucher v WHERE v.client.id = :clientId AND v.status = :status AND v.id <> :excludeId")
  long countByClientAndStatusExcludingId(
      @Param("clientId") UUID clientId, @Param("status") Integer status, @Param("excludeId") UUID excludeId);

  @Query("SELECT COUNT(v) FROM Voucher v WHERE v.client.id = :clientId AND v.status IN :statuses")
  long countByClientAndStatusIn(@Param("clientId") UUID clientId, @Param("statuses") List<Integer> statuses);

  @Query("FROM Voucher v WHERE v.visitDate <= :visitDate AND v.status IN :statuses")
  List<Voucher> findByVisitDateLessThanEqualAndStatusIn(
      @Param("visitDate") LocalDate visitDate, @Param("statuses") List<Integer> statuses);
}
