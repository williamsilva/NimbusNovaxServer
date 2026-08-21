package com.nimbusnovax.common.web;

import java.util.List;
import java.util.Map;

/**
 * Espelha ListQueryDto do frontend (tabela PrimeNG + painel de filtros avançados) - mesma forma de
 * {@code com.nimbusnovax.common.security.bff.admin.AdminSearchRequest}, mas em pacote neutro
 * (com.nimbusnovax.common.web) pra ser reaproveitado por qualquer módulo de negócio (works hoje;
 * tickets/tasks/actionplans no futuro - ver com.nimbusnovax.works.controller.SupplierController et
 * al.), sem acoplar esses módulos ao pacote de administração do NimbusAuth. tableFilters e advanced
 * ficam como Map crus (formatos dinâmicos - ver ColumnFilterDto/TAdvanced no frontend); a extração
 * dos valores é feita ad-hoc em cada Service, só pros campos que a tela de fato filtra (ver
 * FilterSupport).
 */
public record SearchRequest(
    Integer page,
    Integer size,
    List<SortItem> sort,
    Map<String, Object> tableFilters,
    String globalFilter,
    Map<String, Object> advanced) {

  public record SortItem(String field, Integer order) {
  }
}
