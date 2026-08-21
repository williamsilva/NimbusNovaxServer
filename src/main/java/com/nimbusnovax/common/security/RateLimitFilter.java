package com.nimbusnovax.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limiting básico por IP em memória (contador de janela fixa), aplicado só a /bff/** e
 * /api/**. Instância única de app (sem múltiplas réplicas), então estado em memória é suficiente
 * pro estágio atual - se o NimbusNovax rodar com múltiplas instâncias no futuro, isso precisa
 * migrar pra um contador compartilhado (ex.: Redis). Bean simples (@Component implementando
 * Filter) - o Spring Boot registra automaticamente no chain de filtros do servlet container,
 * fora das SecurityFilterChain do Spring Security (não interfere com elas).
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

  private final RateLimitProperties props;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!props.isEnabled() || !isRateLimitedPath(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String key = request.getRemoteAddr();
    Window window = windows.computeIfAbsent(key, k -> new Window(Instant.now().getEpochSecond()));

    if (window.incrementAndCheck(props.getWindowSeconds(), props.getCapacity())) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
  }

  private boolean isRateLimitedPath(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/bff/") || path.startsWith("/api/");
  }

  /** Janela fixa: reseta a contagem sempre que passa windowSeconds desde o início da janela atual. */
  private static final class Window {
    private volatile long windowStartEpochSeconds;
    private final AtomicInteger count = new AtomicInteger(0);

    Window(long windowStartEpochSeconds) {
      this.windowStartEpochSeconds = windowStartEpochSeconds;
    }

    synchronized boolean incrementAndCheck(int windowSeconds, int capacity) {
      long now = Instant.now().getEpochSecond();
      if (now - windowStartEpochSeconds >= windowSeconds) {
        windowStartEpochSeconds = now;
        count.set(0);
      }
      return count.incrementAndGet() <= capacity;
    }
  }
}
