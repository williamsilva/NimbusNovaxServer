package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.model.Product;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import com.nimbussystems.commons.web.FilterSupport;
import com.nimbussystems.commons.web.SearchRequest;
import com.nimbusnovax.common.web.spec.BaseSpecificationSupport;
import com.nimbusnovax.common.web.spec.Specs;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Constrói a {@link Specification} de busca de Product a partir de um {@link SearchRequest} -
 *  mesmo padrão de {@link com.nimbusnovax.voucher.core.VoucherSpecs}. Sem fetch join - Product
 *  não tem nenhuma associação. */
@Component
public class ProductSpecs extends BaseSpecificationSupport<Product> {

  public Specification<Product> fromRequest(SearchRequest request) {
    var tableFilters = request.tableFilters();
    var advanced = request.advanced();

    String name = FilterSupport.textFilter(tableFilters, advanced, "name", "name");
    List<String> typeValues = FilterSupport.listFilter(tableFilters, advanced, "typeProduct", "typeProduct");
    List<String> statusValues = FilterSupport.listFilter(tableFilters, advanced, "status", "status");

    Specification<Product> spec = Specs.all();
    spec = spec.and(contains(name, "name"));
    spec = spec.and(inCodes("typeProduct", typeValues, this::typeCodeFromName));
    spec = spec.and(inCodes("status", statusValues, this::statusCodeFromName));

    return spec;
  }

  private Integer typeCodeFromName(String name) {
    try {
      return TypeProductEnum.toCode(TypeProductEnum.valueOf(name));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private Integer statusCodeFromName(String name) {
    try {
      return StatusEnum.toCode(StatusEnum.valueOf(name));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
