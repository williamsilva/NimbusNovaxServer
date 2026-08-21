package com.nimbusnovax.common.security.bff.admin;

import java.util.List;
import java.util.Map;

/**
 * Espelha ListQueryDto do frontend (tabela PrimeNG + painel de filtros avançados). tableFilters e
 * advanced ficam como Map crus (formatos dinâmicos - ver ColumnFilterDto/TAdvanced no frontend);
 * a extração dos valores é feita ad-hoc em cada Service, só pros campos que as telas de
 * Usuários/Grupos de fato filtram (ver AdminFilterSupport).
 */
public record AdminSearchRequest(
    Integer page,
    Integer size,
    List<SortItem> sort,
    Map<String, Object> tableFilters,
    String globalFilter,
    Map<String, Object> advanced) {

  public record SortItem(String field, Integer order) {
  }
}
