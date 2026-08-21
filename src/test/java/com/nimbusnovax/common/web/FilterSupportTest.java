package com.nimbusnovax.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FilterSupportTest {

  @Test
  void periodLocalDateRange_day_resolvesSingleDayRange() {
    Map<String, Object> advanced = Map.of("periodStartDate", "DAY", "startDate", "15/03/2026");

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 3, 15));
  }

  @Test
  void periodLocalDateRange_start_isOpenEndedUpward() {
    Map<String, Object> advanced = Map.of("periodStartDate", "START", "startDate", "15/03/2026");

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(LocalDate.of(2026, 3, 15), null);
  }

  @Test
  void periodLocalDateRange_end_isOpenEndedDownward() {
    Map<String, Object> advanced = Map.of("periodStartDate", "END", "startDate", "15/03/2026");

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(null, LocalDate.of(2026, 3, 15));
  }

  @Test
  void periodLocalDateRange_month_resolvesFirstToLastDayOfMonth() {
    Map<String, Object> advanced = Map.of("periodStartDate", "MONTH", "startDate", "03/2026");

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
  }

  @Test
  void periodLocalDateRange_year_resolvesJanFirstToDecLast() {
    Map<String, Object> advanced = Map.of("periodStartDate", "YEAR", "startDate", "2026");

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
  }

  @Test
  void periodLocalDateRange_interval_resolvesInOrder() {
    Map<String, Object> advanced =
        Map.of("periodStartDate", "INTERVAL", "startDate", List.of("01/03/2026", "31/03/2026"));

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
  }

  @Test
  void periodLocalDateRange_interval_swapsWhenEndBeforeStart() {
    Map<String, Object> advanced =
        Map.of("periodStartDate", "INTERVAL", "startDate", List.of("31/03/2026", "01/03/2026"));

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
  }

  @Test
  void periodLocalDateRange_nullPeriod_returnsNull() {
    Map<String, Object> advanced = Map.of("periodStartDate", "NULL", "startDate", "15/03/2026");

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).isNull();
  }

  @Test
  void periodLocalDateRange_missingAdvanced_returnsNull() {
    LocalDate[] range = FilterSupport.periodLocalDateRange(null, null, "startDate", "periodStartDate", "startDate");

    assertThat(range).isNull();
  }

  @Test
  void periodLocalDateRange_unparseableValue_returnsNullInsteadOfThrowing() {
    Map<String, Object> advanced = Map.of("periodStartDate", "DAY", "startDate", "not-a-date");

    LocalDate[] range = FilterSupport.periodLocalDateRange(null, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).isNull();
  }

  @Test
  void periodLocalDateRange_columnFilterTakesPrecedenceOverAdvancedPeriod() {
    Map<String, Object> tableFilters =
        Map.of("startDate", Map.of("matchMode", "between", "value", List.of("2026-03-01", "2026-03-10")));
    Map<String, Object> advanced = Map.of("periodStartDate", "YEAR", "startDate", "2020");

    LocalDate[] range = FilterSupport.periodLocalDateRange(
        tableFilters, advanced, "startDate", "periodStartDate", "startDate");

    assertThat(range).containsExactly(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 10));
  }

  @Test
  void periodInstantRange_day_resolvesUtcStartAndEndOfDay() {
    Map<String, Object> advanced = Map.of("periodCreatedAt", "DAY", "createdAt", "15/03/2026");

    Instant[] range =
        FilterSupport.periodInstantRange(null, advanced, "createdAt", "periodCreatedAt", "createdAt");

    assertThat(range[0]).isEqualTo(Instant.parse("2026-03-15T00:00:00Z"));
    assertThat(range[1]).isEqualTo(Instant.parse("2026-03-15T23:59:59.999999999Z"));
  }

  @Test
  void withinRange_localDate_respectsOpenEndedBounds() {
    LocalDate[] fromOnly = {LocalDate.of(2026, 1, 1), null};

    assertThat(FilterSupport.withinRange(LocalDate.of(2026, 6, 1), fromOnly)).isTrue();
    assertThat(FilterSupport.withinRange(LocalDate.of(2025, 12, 31), fromOnly)).isFalse();
  }
}
