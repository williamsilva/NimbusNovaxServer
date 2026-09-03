package com.nimbusnovax.voucher.core;

import com.nimbussystems.commons.web.FilterSupport;
import com.nimbussystems.commons.web.SearchRequest;
import com.nimbusnovax.common.web.spec.BaseSpecificationSupport;
import com.nimbusnovax.common.web.spec.Specs;
import com.nimbusnovax.voucher.model.Voucher;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Constrói a {@link Specification} de busca de Voucher a partir de um {@link SearchRequest} —
 * mesmos campos/precedência (coluna &gt; painel avançado) já resolvidos por {@link FilterSupport},
 * agora aplicados no banco (WHERE real) em vez de num {@code Stream.filter} em memória.
 */
@Component
public class VoucherSpecs extends BaseSpecificationSupport<Voucher> {

  /** Status escondidos da listagem por padrão (voucher já "resolvido") - mesmo critério de
   *  {@code VoucherService.HIDDEN_BY_DEFAULT}: só aparecem quando o usuário filtra por status
   *  explicitamente. */
  private static final Set<Integer> HIDDEN_BY_DEFAULT = Set.of(
      StatusVoucherEnum.EXCHANGED.getCode(), StatusVoucherEnum.CALLED_OFF.getCode(),
      StatusVoucherEnum.NOT_CLOSED.getCode());

  public Specification<Voucher> fromRequest(SearchRequest request) {
    var tableFilters = request.tableFilters();
    var advanced = request.advanced();

    String code = FilterSupport.textFilter(tableFilters, advanced, "voucher", "voucher");
    String clientName = FilterSupport.textFilter(tableFilters, advanced, "client", "client");
    List<String> promoterIds = FilterSupport.listFilter(tableFilters, advanced, "promoter", "promoter");
    List<String> statusValues = FilterSupport.listFilter(tableFilters, advanced, "status", "status");
    LocalDate[] visitRange = FilterSupport.periodLocalDateRange(
        tableFilters, advanced, "visitDate", "periodVisitDate", "visitDate");

    Specification<Voucher> spec = Specs.all();
    spec = spec.and(contains(code, "code"));
    spec = spec.and(containsPath(clientName, "client", "name"));
    spec = spec.and(inPath(promoterIds, this::parseUuidOrNull, "promoter", "id"));
    spec = spec.and(rangeLocalDate("visitDate", visitRange == null ? null : visitRange[0],
        visitRange == null ? null : visitRange[1]));
    spec = spec.and(statusValues.isEmpty() ? excludeHiddenStatus() : inStatusNames(statusValues));
    spec = spec.and(fetchAssociations());

    return spec;
  }

  private Specification<Voucher> inStatusNames(List<String> names) {
    return inCodes("status", names, this::statusCodeFromName);
  }

  private Integer statusCodeFromName(String name) {
    try {
      return StatusVoucherEnum.valueOf(name).getCode();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private Specification<Voucher> excludeHiddenStatus() {
    return (root, query, cb) -> cb.not(root.get("status").in(HIDDEN_BY_DEFAULT));
  }

  /** Trazidas junto no SELECT (LEFT JOIN FETCH) para evitar N+1 na listagem — nenhuma delas é uma
   *  coleção, então não há risco de duplicar linha/precisar de DISTINCT. */
  private Specification<Voucher> fetchAssociations() {
    return (root, query, cb) -> {
      if (!isCountQuery(query)) {
        fetchIfNotFetched(root, "client");
        fetchIfNotFetched(root, "promoter");
        fetchIfNotFetched(root, "tourGuide");
        fetchIfNotFetched(root, "cancellationReason");
      }
      return cb.conjunction();
    };
  }

  private UUID parseUuidOrNull(String value) {
    try {
      return UUID.fromString(value);
    } catch (Exception e) {
      return null;
    }
  }
}
