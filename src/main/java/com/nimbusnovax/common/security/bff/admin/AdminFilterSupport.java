package com.nimbusnovax.common.security.bff.admin;

import com.nimbusnovax.common.web.FilterSupport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helpers pra extrair valores de filtro dos Maps crus de AdminSearchRequest.tableFilters/advanced.
 * Só cobre os matchModes de fato usados pelas telas de Usuários/Grupos: contains (texto), in
 * (multi-select) e between (intervalo de data) nas colunas da tabela, mais os campos equivalentes
 * do painel "avançado" - ver ListQueryDto/readFilterValues no frontend. Não é um motor genérico
 * de FilterMatchMode.
 */
final class AdminFilterSupport {

  private AdminFilterSupport() {
  }

  /**
   * Primeiro valor não nulo de filters[field], no formato PrimeNG
   * ({@code {operator, constraints:[{matchMode, value}]}}) ou já um array de constraints crus.
   */
  @SuppressWarnings("unchecked")
  private static Object firstTableFilterValue(Map<String, Object> tableFilters, String field) {
    if (tableFilters == null) {
      return null;
    }

    Object raw = tableFilters.get(field);
    if (raw == null) {
      return null;
    }

    List<Object> constraints;
    if (raw instanceof List<?> list) {
      constraints = (List<Object>) list;
    } else if (raw instanceof Map<?, ?> map && map.get("constraints") instanceof List<?> list) {
      constraints = (List<Object>) list;
    } else {
      constraints = List.of(raw);
    }

    for (Object constraint : constraints) {
      Object value = constraint instanceof Map<?, ?> m ? m.get("value") : null;
      if (value != null) {
        return value;
      }
    }

    return null;
  }

  static String textFilter(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedField) {
    Object fromTable = firstTableFilterValue(tableFilters, tableField);
    if (fromTable instanceof String s && !s.isBlank()) {
      return s;
    }

    Object fromAdvanced = advanced == null ? null : advanced.get(advancedField);
    return fromAdvanced instanceof String s && !s.isBlank() ? s : null;
  }

  static List<String> listFilter(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedField) {
    List<String> values = new ArrayList<>();

    Object fromTable = firstTableFilterValue(tableFilters, tableField);
    if (fromTable instanceof List<?> list) {
      list.forEach(v -> addUnique(values, v));
    }

    Object fromAdvanced = advanced == null ? null : advanced.get(advancedField);
    if (fromAdvanced instanceof List<?> list) {
      list.forEach(v -> addUnique(values, v));
    }

    return values;
  }

  private static void addUnique(List<String> values, Object raw) {
    if (raw == null) {
      return;
    }
    String value = String.valueOf(raw);
    if (!values.contains(value)) {
      values.add(value);
    }
  }

  /**
   * Mesmo padrão período+valor de {@link FilterSupport#periodInstantRange} - delegado pra lá pra
   * não duplicar o parser de data/mês/ano (mesma lógica usada pelas telas de negócio, ver
   * WorkService/SupplierService); só reexportado aqui pra manter a convenção desta classe de ser o
   * único ponto de leitura de filtro usado pelos services de Usuário/Grupo.
   */
  static Instant[] periodInstantRange(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedPeriodField, String advancedValueField) {
    return FilterSupport.periodInstantRange(
        tableFilters, advanced, tableField, advancedPeriodField, advancedValueField);
  }

  static boolean containsIgnoreCase(String haystack, String needle) {
    if (needle == null || needle.isBlank()) {
      return true;
    }
    return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
  }

  static boolean withinRange(Instant value, Instant[] range) {
    if (range == null) {
      return true;
    }
    if (value == null) {
      return false;
    }

    Instant from = range[0];
    Instant to = range[1];

    if (from != null && value.isBefore(from)) {
      return false;
    }
    return to == null || !value.isAfter(to);
  }
}
