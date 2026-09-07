package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbussystems.commons.web.FilterSupport;
import com.nimbussystems.commons.web.SearchRequest;
import com.nimbusnovax.common.web.spec.BaseSpecificationSupport;
import com.nimbusnovax.common.web.spec.Specs;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Ver {@link com.nimbusnovax.voucher.core.VoucherSpecs} para o padrão. */
@Component
public class CancellationReasonSpecs extends BaseSpecificationSupport<CancellationReason> {

  public Specification<CancellationReason> fromRequest(SearchRequest request) {
    var tableFilters = request.tableFilters();
    var advanced = request.advanced();

    String name = FilterSupport.textFilter(tableFilters, advanced, "name", "name");
    List<String> statusValues = FilterSupport.listFilter(tableFilters, advanced, "status", "status");

    Specification<CancellationReason> spec = Specs.all();
    spec = spec.and(contains(name, "name"));
    spec = spec.and(inCodes("status", statusValues, this::statusCodeFromName));

    return spec;
  }

  private Integer statusCodeFromName(String name) {
    try {
      return StatusEnum.toCode(StatusEnum.valueOf(name));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
