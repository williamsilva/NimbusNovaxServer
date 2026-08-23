package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import com.nimbusnovax.common.web.FilterSupport;
import com.nimbusnovax.common.web.SearchRequest;
import com.nimbusnovax.common.web.spec.BaseSpecificationSupport;
import com.nimbusnovax.common.web.spec.Specs;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/** Ver {@link com.nimbusnovax.voucher.core.VoucherSpecs} para o padrão. Agent não tem nenhuma
 *  relação ManyToOne (só coleções OneToMany - agentTypes/addresses/contacts - que a listagem não
 *  usa, ver {@link com.nimbusnovax.administracao.representation.AgentModelAssembler}), então não
 *  há fetch join a fazer aqui. */
@Component
public class AgentSpecs extends BaseSpecificationSupport<Agent> {

  public Specification<Agent> fromRequest(SearchRequest request) {
    var tableFilters = request.tableFilters();
    var advanced = request.advanced();

    String code = FilterSupport.textFilter(tableFilters, advanced, "code", "code");
    String name = FilterSupport.textFilter(tableFilters, advanced, "name", "name");
    String document = FilterSupport.textFilter(tableFilters, advanced, "document", "document");
    List<String> typePersonValues = FilterSupport.listFilter(tableFilters, advanced, "typePerson", "typePerson");
    Instant[] createdAtRange = FilterSupport.periodInstantRange(
        tableFilters, advanced, "createdAt", "periodCreatedAt", "createdAt");

    Specification<Agent> spec = Specs.all();
    spec = spec.and(contains(code, "code"));
    spec = spec.and(contains(name, "name"));
    spec = spec.and(contains(document, "document"));
    spec = spec.and(inCodes("typePerson", typePersonValues, this::typePersonCodeFromName));
    spec = spec.and(rangeInstant("createdAt", createdAtRange == null ? null : createdAtRange[0],
        createdAtRange == null ? null : createdAtRange[1]));

    return spec;
  }

  private Integer typePersonCodeFromName(String name) {
    try {
      return TypePersonEnum.toCode(TypePersonEnum.valueOf(name));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
