package com.nimbusnovax.common.security;

import com.nimbusnovax.common.security.bff.ChangePasswordRequest;
import com.nimbusnovax.common.security.bff.PasswordPolicyResponse;
import com.nimbusnovax.common.security.bff.ProfileResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Chamadas server-to-server pro NimbusAuth (perfil próprio e senha) - mesmo papel do
 * BffApiClient/PasswordPolicyProxyController do CardSyncServer, só que com DTOs tipados (não
 * repassa o JSON bruto do NimbusAuth pro browser) e injetando o username do usuário autenticado
 * na checagem de política em vez de aceitar um vindo do cliente. Timeout de conexão/leitura vem
 * do RestClientCustomizer global (ver com.nimbusnovax.common.config.RestClientTimeoutConfig) -
 * não é setado aqui pra não sobrescrever o MockServerRestClientCustomizer que @RestClientTest
 * injeta no RestClient.Builder (ver NimbusAuthClientTest).
 */
@Service
public class NimbusAuthClient {

  private static final Logger log = LoggerFactory.getLogger(NimbusAuthClient.class);

  private final RestClient restClient;

  public NimbusAuthClient(RestClient.Builder restClientBuilder, NimbusAuthProxyProperties props) {
    this.restClient = restClientBuilder.baseUrl(props.getBaseUrl()).build();
  }

  /** GET /api/v1/me/profile (autenticado - self-service, ver MeProfileController no NimbusAuth). */
  public ProfileResponse getMyProfile(String accessToken) {
    try {
      return restClient.get()
          .uri("/api/v1/me/profile")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(ProfileResponse.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    } catch (ResourceAccessException e) {
      throw upstreamUnavailable(e);
    }
  }

  /** GET /api/password/policy (público no NimbusAuth - sem Bearer). */
  public PasswordPolicyResponse getPasswordPolicy() {
    try {
      return restClient.get().uri("/api/password/policy").retrieve().body(PasswordPolicyResponse.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    } catch (ResourceAccessException e) {
      throw upstreamUnavailable(e);
    }
  }

  /** POST /api/password/policy/check (público no NimbusAuth - sem Bearer). */
  public PasswordPolicyResponse checkPasswordPolicy(String password, String confirmPassword, String username) {
    try {
      return restClient.post()
          .uri("/api/password/policy/check")
          .contentType(MediaType.APPLICATION_JSON)
          .body(new NimbusAuthPasswordCheckRequest(password, confirmPassword, username))
          .retrieve()
          .body(PasswordPolicyResponse.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    } catch (ResourceAccessException e) {
      throw upstreamUnavailable(e);
    }
  }

  /** PUT /api/v1/me/password/change (autenticado - self-service). */
  public void changeMyPassword(String accessToken, ChangePasswordRequest request) {
    try {
      restClient.put()
          .uri("/api/v1/me/password/change")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    } catch (ResourceAccessException e) {
      throw upstreamUnavailable(e);
    }
  }

  private record NimbusAuthPasswordCheckRequest(String password, String confirmPassword, String username) {
  }

  /**
   * Repassa o "code" de erro do NimbusAuth (ex.: PASSWORD_CURRENT_INVALID,
   * PASSWORD_POLICY_INVALID) como reason de um 400 - o frontend traduz esse código via
   * errors.<code> (ver i18n). Qualquer outra falha (rede, 5xx, formato inesperado) vira um 502
   * genérico - não expõe detalhe interno do NimbusAuth pro cliente.
   */
  private ResponseStatusException mapUpstreamError(RestClientResponseException e) {
    if (e.getStatusCode().value() == HttpStatus.BAD_REQUEST.value()) {
      String code = extractErrorCode(e);
      if (code != null) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
      }
    }
    // Sem isto, um 502/500/403 inesperado do NimbusAuth vira um 502 NIMBUS_AUTH_ERROR genérico
    // pro cliente sem deixar rastro nenhum de qual foi a falha real - foi assim que um bug real
    // (ver upstreamUnavailable) ficou invisível em produção por várias rodadas de investigação.
    log.error("Falha inesperada do NimbusAuth: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
    return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "NIMBUS_AUTH_ERROR");
  }

  /** Falha de rede/timeout (sem resposta HTTP nenhuma pra inspecionar status/body, ao contrário
   *  de mapUpstreamError) - mesmo código genérico NIMBUS_AUTH_ERROR do caso "qualquer outra
   *  falha" acima. Loga com stacktrace (causa real: connection refused, timeout, DNS, etc.) -
   *  antes desta chamada, essa exceção era simplesmente engolida sem deixar nenhum traço nos
   *  logs, tornando impossível diagnosticar falhas reais de conectividade com o NimbusAuth. */
  private ResponseStatusException upstreamUnavailable(ResourceAccessException e) {
    log.error("Falha de rede chamando o NimbusAuth", e);
    return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "NIMBUS_AUTH_ERROR");
  }

  private String extractErrorCode(RestClientResponseException e) {
    try {
      Map<?, ?> body = e.getResponseBodyAs(Map.class);
      Object code = body != null ? body.get("code") : null;
      return code != null ? code.toString() : null;
    } catch (Exception ex) {
      return null;
    }
  }
}
