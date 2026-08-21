package com.nimbusnovax.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * Padrão recomendado pela própria documentação do Spring Security para SPA lendo o token via
 * cookie (não via campo de formulário renderizado no HTML): o Angular lê o valor bruto do cookie
 * XSRF-TOKEN e o devolve verbatim no header X-XSRF-TOKEN, então a leitura do lado servidor não
 * pode esperar o valor "mascarado" (XOR) que o handler default usa para me proteger de BREACH.
 */
public final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {

  private final CsrfTokenRequestHandler delegate = new XorCsrfTokenRequestAttributeHandler();

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
    delegate.handle(request, response, csrfToken);
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
    String headerValue = request.getHeader(csrfToken.getHeaderName());
    return StringUtils.hasText(headerValue)
      ? super.resolveCsrfTokenValue(request, csrfToken)
      : delegate.resolveCsrfTokenValue(request, csrfToken);
  }
}
