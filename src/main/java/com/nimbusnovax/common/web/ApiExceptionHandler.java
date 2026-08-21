package com.nimbusnovax.common.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * BffAccessTokenService.getValidAccessToken lança OAuth2AuthorizationException quando o
 * authorized client OAuth2 (access/refresh token) da sessão não é mais válido - típico depois de
 * um restart do backend: a sessão HTTP sobrevive via spring-session-jdbc, mas o authorized client
 * em si não. Essa exceção estende RuntimeException diretamente (não AuthenticationException), então
 * o authenticationEntryPoint(HttpStatusEntryPoint(UNAUTHORIZED)) da SecurityConfig não a intercepta
 * - sem este handler, ela sobe até o BasicErrorController padrão do Spring Boot como 500 genérico,
 * e o auth-redirect.interceptor.ts do frontend (que só reage a 401/403) nunca dispara o re-login
 * automático. Mapeado pra 401 de propósito, mesmo princípio já aplicado no CardsyncServer.
 *
 * <p>Só esse caso é tratado aqui - o resto do app continua usando ResponseStatusException direto
 * (resolvido nativamente pelo Spring, sem handler customizado), então este advice não deve ganhar
 * um catch-all genérico sem antes auditar todos os fluxos que hoje dependem do comportamento
 * default do Spring pra ResponseStatusException.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(OAuth2AuthorizationException.class)
  public ProblemDetail handleOAuth2AuthorizationRequired(OAuth2AuthorizationException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED");
  }
}
