package com.nimbusnovax.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * O token CSRF só é resolvido/renderizado sob demanda (proteção contra BREACH) — sem "tocar" nele
 * a cada request, o cookie XSRF-TOKEN nunca chega a ser escrito na resposta, e o Angular nunca tem
 * o valor pra devolver no header. Este filtro força essa resolução em toda requisição do chain BFF.
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      csrfToken.getToken();
    }
    filterChain.doFilter(request, response);
  }
}
