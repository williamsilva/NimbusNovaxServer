package com.nimbusnovax.common.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helpers pra extrair valores de filtro dos Maps crus de SearchRequest.tableFilters/advanced -
 * mesma ideia de {@code com.nimbusnovax.common.security.bff.admin.AdminFilterSupport} (ver
 * {@link SearchRequest} para o porquê deste pacote separado), com um helper extra de intervalo
 * decimal (usado por Obra.totalAmount) que a tela de Usuários/Grupos nunca precisou. Só cobre os
 * matchModes de fato usados pelas telas atuais: contains (texto), in (multi-select) e between
 * (intervalo de data/número) - não é um motor genérico de FilterMatchMode.
 */
public final class FilterSupport {

  private FilterSupport() {
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

  public static String textFilter(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedField) {
    Object fromTable = firstTableFilterValue(tableFilters, tableField);
    if (fromTable instanceof String s && !s.isBlank()) {
      return s;
    }

    Object fromAdvanced = advanced == null ? null : advanced.get(advancedField);
    return fromAdvanced instanceof String s && !s.isBlank() ? s : null;
  }

  public static List<String> listFilter(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedField) {
    List<String> values = new ArrayList<>();

    Object fromTable = firstTableFilterValue(tableFilters, tableField);
    if (fromTable instanceof List<?> list) {
      list.forEach(v -> addUnique(values, v));
    } else if (fromTable != null) {
      addUnique(values, fromTable);
    }

    Object fromAdvanced = advanced == null ? null : advanced.get(advancedField);
    if (fromAdvanced instanceof List<?> list) {
      list.forEach(v -> addUnique(values, v));
    } else if (fromAdvanced != null) {
      addUnique(values, fromAdvanced);
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

  /** Mesma ideia de {@link #periodInstantRange}, mas pra colunas numéricas (ex.: Work.totalAmount). */
  public static BigDecimal[] decimalRangeFilter(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedFromField, String advancedToField) {

    BigDecimal from = null;
    BigDecimal to = null;

    Object fromTable = firstTableFilterValue(tableFilters, tableField);
    if (fromTable instanceof List<?> range && range.size() == 2) {
      from = parseDecimal(range.get(0));
      to = parseDecimal(range.get(1));
    }

    if (advanced != null) {
      if (from == null) {
        from = parseDecimal(advanced.get(advancedFromField));
      }
      if (to == null) {
        to = parseDecimal(advanced.get(advancedToField));
      }
    }

    return (from == null && to == null) ? null : new BigDecimal[] {from, to};
  }

  private static LocalDate parseLocalDate(Object value) {
    if (value == null) {
      return null;
    }
    String raw = String.valueOf(value);
    try {
      return LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  /** Espelha o PeriodEnum do frontend (ver period.enum.ts) - só usado internamente pelo parser abaixo. */
  private enum Period {
    NULL, DAY, END, YEAR, MONTH, START, INTERVAL
  }

  private static final DateTimeFormatter PERIOD_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter PERIOD_MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");

  /**
   * Intervalo de datas no padrão período+valor do CardSync (mesmo componente
   * {@code cs-advanced-period-date-filter} do frontend): a coluna continua mandando um array
   * {@code [from, to]} num único campo (matchMode "between", ver {@link #firstTableFilterValue});
   * o painel avançado agora manda o par período (advancedPeriodField, ex. "periodStartDate") +
   * valor (advancedValueField, ex. "startDate") em vez de dois campos from/to separados. Se a
   * coluna já resolveu algo, o painel avançado não é consultado (o período resolve um par atômico,
   * não dois limites independentes pra mesclar). Retorna {@code null} se nenhum dos dois mandou
   * nada, ou se o valor do período não pôde ser interpretado.
   */
  public static LocalDate[] periodLocalDateRange(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedPeriodField, String advancedValueField) {

    LocalDate from = null;
    LocalDate to = null;

    Object fromTable = firstTableFilterValue(tableFilters, tableField);
    if (fromTable instanceof List<?> range && range.size() == 2) {
      from = parseLocalDate(range.get(0));
      to = parseLocalDate(range.get(1));
    }

    if (from == null && to == null) {
      LocalDate[] periodRange = resolvePeriodRange(
          readPeriod(advanced, advancedPeriodField), readPeriodValues(advanced, advancedValueField));
      if (periodRange != null) {
        from = periodRange[0];
        to = periodRange[1];
      }
    }

    return (from == null && to == null) ? null : new LocalDate[] {from, to};
  }

  /** Mesma ideia de {@link #periodLocalDateRange}, mas pra colunas {@code Instant} (ex.: createdAt). */
  public static Instant[] periodInstantRange(
      Map<String, Object> tableFilters, Map<String, Object> advanced,
      String tableField, String advancedPeriodField, String advancedValueField) {

    Instant from = null;
    Instant to = null;

    Object fromTable = firstTableFilterValue(tableFilters, tableField);
    if (fromTable instanceof List<?> range && range.size() == 2) {
      from = parseInstant(range.get(0));
      to = parseInstant(range.get(1));
    }

    if (from == null && to == null) {
      LocalDate[] periodRange = resolvePeriodRange(
          readPeriod(advanced, advancedPeriodField), readPeriodValues(advanced, advancedValueField));
      if (periodRange != null) {
        from = periodRange[0] == null ? null : periodRange[0].atStartOfDay(ZoneOffset.UTC).toInstant();
        to = periodRange[1] == null ? null
            : periodRange[1].plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant();
      }
    }

    return (from == null && to == null) ? null : new Instant[] {from, to};
  }

  private static Period readPeriod(Map<String, Object> advanced, String field) {
    Object raw = advanced == null ? null : advanced.get(field);
    if (raw == null) {
      return null;
    }
    try {
      return Period.valueOf(String.valueOf(raw).trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static List<String> readPeriodValues(Map<String, Object> advanced, String field) {
    Object raw = advanced == null ? null : advanced.get(field);
    if (raw == null) {
      return List.of();
    }
    if (raw instanceof List<?> list) {
      List<String> values = new ArrayList<>();
      for (Object v : list) {
        if (v != null) {
          values.add(String.valueOf(v));
        }
      }
      return values;
    }
    return List.of(String.valueOf(raw));
  }

  private static LocalDate[] resolvePeriodRange(Period period, List<String> values) {
    if (period == null || period == Period.NULL || values.isEmpty()) {
      return null;
    }
    return switch (period) {
      case DAY -> {
        LocalDate d = parsePeriodDate(values.get(0));
        yield d == null ? null : new LocalDate[] {d, d};
      }
      case START -> {
        LocalDate d = parsePeriodDate(values.get(0));
        yield d == null ? null : new LocalDate[] {d, null};
      }
      case END -> {
        LocalDate d = parsePeriodDate(values.get(0));
        yield d == null ? null : new LocalDate[] {null, d};
      }
      case MONTH -> {
        YearMonth m = parsePeriodMonth(values.get(0));
        yield m == null ? null : new LocalDate[] {m.atDay(1), m.atEndOfMonth()};
      }
      case YEAR -> {
        Year y = parsePeriodYear(values.get(0));
        yield y == null ? null : new LocalDate[] {y.atDay(1), y.atMonth(12).atEndOfMonth()};
      }
      case INTERVAL -> {
        if (values.size() < 2) {
          yield null;
        }
        LocalDate start = parsePeriodDate(values.get(0));
        LocalDate end = parsePeriodDate(values.get(1));
        if (start == null || end == null) {
          yield null;
        }
        yield end.isBefore(start) ? new LocalDate[] {end, start} : new LocalDate[] {start, end};
      }
      default -> null;
    };
  }

  private static LocalDate parsePeriodDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(raw.trim(), PERIOD_DATE_FORMAT);
    } catch (DateTimeParseException e) {
      try {
        return LocalDate.parse(raw.trim());
      } catch (DateTimeParseException e2) {
        return null;
      }
    }
  }

  private static YearMonth parsePeriodMonth(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return YearMonth.parse(raw.trim(), PERIOD_MONTH_FORMAT);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static Year parsePeriodYear(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      return Year.parse(raw.trim());
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public static boolean withinRange(LocalDate value, LocalDate[] range) {
    if (range == null) {
      return true;
    }
    if (value == null) {
      return false;
    }

    LocalDate from = range[0];
    LocalDate to = range[1];

    if (from != null && value.isBefore(from)) {
      return false;
    }
    return to == null || !value.isAfter(to);
  }

  private static Instant parseInstant(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(String.valueOf(value));
    } catch (Exception e) {
      return null;
    }
  }

  private static BigDecimal parseDecimal(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return new BigDecimal(String.valueOf(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static boolean containsIgnoreCase(String haystack, String needle) {
    if (needle == null || needle.isBlank()) {
      return true;
    }
    return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
  }

  public static boolean withinRange(Instant value, Instant[] range) {
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

  public static boolean withinRange(BigDecimal value, BigDecimal[] range) {
    if (range == null) {
      return true;
    }
    if (value == null) {
      return false;
    }

    BigDecimal from = range[0];
    BigDecimal to = range[1];

    if (from != null && value.compareTo(from) < 0) {
      return false;
    }
    return to == null || value.compareTo(to) <= 0;
  }
}
