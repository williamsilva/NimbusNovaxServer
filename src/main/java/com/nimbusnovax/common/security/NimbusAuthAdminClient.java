package com.nimbusnovax.common.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Chamadas server-to-server pro NimbusAuth - administração de usuários e grupos (não self-service,
 * ver NimbusAuthClient pra isso). Sempre escopado a app_key="nimbusnovax" nas leituras (grupos e
 * permissões são catalogados por app, ver GroupService no NimbusAuth); usuários são globais no
 * NimbusAuth (sem app_key), o filtro pro que "pertence ao NimbusNovax" é feito em AdminUserService
 * (pelo appKey dos grupos do usuário), não aqui - este client só espelha o wire format cru.
 *
 * <p>Sem paginação real: /api/v1/users/search é chamado com uma página só (tamanho grande) - o
 * volume esperado de usuários/grupos vinculados ao NimbusNovax é pequeno. Se isso deixar de ser
 * verdade, revisar (ver PAGE_SIZE).
 */
@Slf4j
@Service
public class NimbusAuthAdminClient {

  private static final String APP_KEY = "nimbusnovax";
  private static final int PAGE_SIZE = 500;

  private final RestClient restClient;

  public NimbusAuthAdminClient(RestClient.Builder restClientBuilder, NimbusAuthProxyProperties props) {
    this.restClient = restClientBuilder.baseUrl(props.getBaseUrl()).build();
  }

  // ------------------------- Usuários -------------------------

  /** POST /api/v1/users/search - sem filtro (todos os usuários globais do NimbusAuth; o recorte
   *  "vinculado ao NimbusNovax" é aplicado depois, em AdminUserService, pelo appKey dos groups). */
  public List<RawUser> searchAllUsers(String accessToken) {
    try {
      HalPage<RawUser> page = restClient.post()
          .uri("/api/v1/users/search")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(new SearchRequest(0, PAGE_SIZE))
          .retrieve()
          .body(new org.springframework.core.ParameterizedTypeReference<HalPage<RawUser>>() {});
      return page == null ? List.of() : page.items();
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public RawUser createUser(String accessToken, RawUserInput input) {
    try {
      return restClient.post()
          .uri("/api/v1/users")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(input)
          .retrieve()
          .body(RawUser.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public RawUser getUser(String accessToken, UUID id) {
    try {
      return restClient.get()
          .uri("/api/v1/users/{id}", id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(RawUser.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public RawUser updateUser(String accessToken, UUID id, RawUserInput input) {
    try {
      return restClient.put()
          .uri("/api/v1/users/{id}", id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(input)
          .retrieve()
          .body(RawUser.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public void activateUser(String accessToken, UUID id) {
    postAction(accessToken, "/api/v1/users/{id}/activate", id);
  }

  public void deactivateUser(String accessToken, UUID id) {
    postAction(accessToken, "/api/v1/users/{id}/deactivate", id);
  }

  public void resendInvite(String accessToken, UUID id) {
    postAction(accessToken, "/api/v1/users/{id}/resend-invite", id);
  }

  /** POST /api/v1/users/activate - ListIdsInput ({"ids": [...]}), não confundir com o
   *  activateUser() singular (rota .../{id}/activate). */
  public void activateUsersBulk(String accessToken, List<UUID> ids) {
    bulkAction(accessToken, "/api/v1/users/activate", ids);
  }

  public void deactivateUsersBulk(String accessToken, List<UUID> ids) {
    bulkAction(accessToken, "/api/v1/users/deactivate", ids);
  }

  private void bulkAction(String accessToken, String uri, List<UUID> ids) {
    try {
      restClient.post()
          .uri(uri)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("ids", ids))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  private void postAction(String accessToken, String uri, UUID id) {
    try {
      restClient.post()
          .uri(uri, id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  // ------------------------- Grupos -------------------------

  /** GET /api/v1/groups/options?appKey=nimbusnovax - versão leve (sem contadores/data), usada só
   *  pro multiselect de grupos do formulário de usuário. Pra listagem em si, ver searchAllGroups. */
  public List<RawGroupOption> listGroupOptions(String accessToken) {
    try {
      List<RawGroupOption> options = restClient.get()
          .uri("/api/v1/groups/options?appKey={appKey}", APP_KEY)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(new org.springframework.core.ParameterizedTypeReference<List<RawGroupOption>>() {});
      return options == null ? List.of() : options;
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  /** POST /api/v1/groups/search - filtrado por appKey=nimbusnovax (só grupos do NimbusNovax, ao
   *  contrário de searchAllUsers). Versão completa (com contadores/data/autor) usada na listagem. */
  public List<RawGroup> searchAllGroups(String accessToken) {
    try {
      HalPage<RawGroup> page = restClient.post()
          .uri("/api/v1/groups/search")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(new GroupsSearchRequest(0, PAGE_SIZE, new GroupsSearchFilter(APP_KEY)))
          .retrieve()
          .body(new org.springframework.core.ParameterizedTypeReference<HalPage<RawGroup>>() {});
      return page == null ? List.of() : page.items();
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  // ------------------------- Auditoria de e-mail -------------------------

  /** POST /api/v1/email-logs - convite/reset de senha do próprio NimbusAuth, filtrado a
   *  recipientAppKey=nimbusnovax (só quem pertence hoje a algum grupo do NimbusNovax, ver
   *  EmailLogSpecs.recipientBelongsToApp no NimbusAuth). Degrada silenciosamente (lista vazia) em
   *  qualquer falha - mesmo espírito de NimbusAuthInternalClient.fetchUsers: isso só complementa a
   *  Auditoria de E-mail (ver EmailLogService.search), nunca deve derrubar a tela inteira só
   *  porque o NimbusAuth está fora do ar ou a permissão AUDIT_MAIL_CONSULT ainda não foi concedida
   *  ao token do usuário (precisa de novo login depois da migration que a seeda). */
  public List<RawEmailLog> searchEmailLogs(String accessToken) {
    try {
      HalPage<RawEmailLog> page = restClient.post()
          .uri("/api/v1/email-logs")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(new EmailLogSearchRequest(0, PAGE_SIZE, new EmailLogSearchFilter(APP_KEY)))
          .retrieve()
          .body(new org.springframework.core.ParameterizedTypeReference<HalPage<RawEmailLog>>() {});
      return page == null ? List.of() : page.items();
    } catch (Exception e) {
      log.warn("Falha ao buscar e-mails de convite/reset de senha no NimbusAuth: {}", e.getMessage());
      return List.of();
    }
  }

  public RawGroup getGroup(String accessToken, UUID id) {
    try {
      return restClient.get()
          .uri("/api/v1/groups/{id}", id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(RawGroup.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public RawGroup createGroup(String accessToken, RawGroupInput input) {
    try {
      return restClient.post()
          .uri("/api/v1/groups")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(input)
          .retrieve()
          .body(RawGroup.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public RawGroup updateGroup(String accessToken, UUID id, RawGroupInput input) {
    try {
      return restClient.put()
          .uri("/api/v1/groups/{id}", id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(input)
          .retrieve()
          .body(RawGroup.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public void deleteGroup(String accessToken, UUID id) {
    try {
      restClient.delete()
          .uri("/api/v1/groups/{id}", id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public RawGroup updateGroupPermissions(String accessToken, UUID id, List<UUID> permissionIds) {
    try {
      return restClient.put()
          .uri("/api/v1/groups/{id}/permissions", id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("permissionIds", permissionIds))
          .retrieve()
          .body(RawGroup.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  public RawGroup updateGroupUsers(String accessToken, UUID id, List<UUID> userIds) {
    try {
      return restClient.put()
          .uri("/api/v1/groups/{id}/users", id)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("userIds", userIds))
          .retrieve()
          .body(RawGroup.class);
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  // ------------------------- Permissões -------------------------

  /** GET /api/v1/permissions/options?appKey=nimbusnovax */
  public List<RawPermissionOption> listPermissionOptions(String accessToken) {
    try {
      List<RawPermissionOption> options = restClient.get()
          .uri("/api/v1/permissions/options?appKey={appKey}", APP_KEY)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(new org.springframework.core.ParameterizedTypeReference<List<RawPermissionOption>>() {});
      return options == null ? List.of() : options;
    } catch (RestClientResponseException e) {
      throw mapUpstreamError(e);
    }
  }

  // ------------------------- Wire format (espelha os *Model/*Input do NimbusAuth) -------------------------

  public record RawGroupOption(UUID id, String name, String description, String appKey) {}

  public record RawPermissionOption(UUID id, String name, String description, String appKey) {}

  /** Espelha UserMinimalModel do NimbusAuth (id/name/userName) - só usado como "quem cadastrou". */
  public record RawUserMinimal(UUID id, String name, String userName) {}

  public record RawGroup(
      UUID id,
      String name,
      String description,
      Integer usersCount,
      Integer permissionsCount,
      Instant createdAt,
      RawUserMinimal createdBy,
      List<RawPermissionOption> permissions,
      List<RawUserMinimal> users) {}

  public record RawGroupInput(String name, String description) {}

  public record RawUser(
      UUID id,
      String name,
      String userName,
      String document,
      Integer status,
      Instant createdAt,
      Instant lastLoginAt,
      Instant blockedUntil,
      Instant passwordExpiresAt,
      RawUserMinimal createdBy,
      List<RawGroupOption> groups) {}

  public record RawUserInput(String userName, String name, String document, List<UUID> groupIds) {}

  /** Espelha com.nimbus.auth.bff.controller.v1.representation.model.EmailLogModel do NimbusAuth -
   *  status/eventType chegam como o nome cru do enum (ex.: "SENT", "PASSWORD_RESET"), mapeados
   *  pra minúsculo/snake_case em EmailLogService pra bater com o padrão já usado no eventType do
   *  próprio email_log do NimbusNovaxServer (ver email-log-event-type.enum.ts no frontend). */
  public record RawEmailLog(
      UUID id,
      String status,
      String eventType,
      String subject,
      String template,
      String recipient,
      String errorMessage,
      String body,
      Instant sentAt) {}

  private record SearchRequest(int page, int size) {}

  private record GroupsSearchFilter(String appKey) {}

  private record GroupsSearchRequest(int page, int size, GroupsSearchFilter advanced) {}

  private record EmailLogSearchFilter(String recipientAppKey) {}

  private record EmailLogSearchRequest(int page, int size, EmailLogSearchFilter advanced) {}

  /**
   * PagedModel do Spring HATEOAS serializa como {"_embedded": {"content": [...]}, "page": {...}}.
   * Só usado pra /users/search (as listagens de grupo/permissão usam os endpoints /options, que
   * devolvem um array simples, sem esse envelope HAL).
   */
  private record HalPage<T>(@JsonProperty("_embedded") Embedded<T> embedded) {
    private record Embedded<T>(List<T> content) {}

    List<T> items() {
      return embedded == null || embedded.content() == null ? List.of() : embedded.content();
    }
  }

  /** Mesmo mapeamento de erro do NimbusAuthClient (perfil/senha) - repassa o "code" de um 4xx
   *  conhecido, qualquer outra falha vira 502 genérico. */
  private ResponseStatusException mapUpstreamError(RestClientResponseException e) {
    HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
    if (status != null && status.is4xxClientError()) {
      String code = extractErrorCode(e);
      if (code != null) {
        return new ResponseStatusException(status, code);
      }
    }
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
