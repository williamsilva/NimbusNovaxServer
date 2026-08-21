package com.nimbusnovax.common.web;

import java.util.List;

/**
 * Espelha HalPagedResponse do frontend (envelope {@code _embedded.content} + {@code page}) - mesma
 * forma de {@code com.nimbusnovax.common.security.bff.admin.AdminPageResponse}, ver
 * {@link SearchRequest} para o porquê deste pacote separado.
 */
public record PageResponse<T>(Embedded<T> _embedded, PageMeta page) {

  public record Embedded<T>(List<T> content) {
  }

  public record PageMeta(
      int page, int size, long totalElements, int totalPages, boolean first, boolean last) {
  }

  public static <T> PageResponse<T> of(List<T> pageContent, int page, int size, long totalElements) {
    int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    boolean first = page <= 0;
    boolean last = page >= totalPages - 1;

    return new PageResponse<>(
        new Embedded<>(pageContent), new PageMeta(page, size, totalElements, totalPages, first, last));
  }
}
