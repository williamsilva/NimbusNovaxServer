package com.nimbusnovax.common.security;

import com.nimbussystems.commons.security.NimbusAuthProxyProperties;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP interno (machine-to-machine) pro NimbusAuth - hoje só resolve nome/username de
 * usuários (auditoria: requestedBy/approvedBy de Aditivo, ver UserDirectoryService), mesmo papel
 * do NimbusAuthInternalClient do CardsyncServer, mesmo endpoint (/internal/users) e mesmo secret
 * compartilhado (NIMBUS_INTERNAL_API_SECRET) - autenticado por header, não por token de usuário.
 */
@Slf4j
@Service
public class NimbusAuthInternalClient {

  public record UserSummary(UUID id, String username, String name) {
  }

  private final RestClient restClient;
  private final String internalApiSecret;

  public NimbusAuthInternalClient(RestClient.Builder restClientBuilder, NimbusAuthProxyProperties props) {
    this.restClient = restClientBuilder.baseUrl(props.getBaseUrl()).build();
    this.internalApiSecret = props.getInternalApiSecret();
  }

  /** Degrada silenciosamente (lista vazia) em qualquer falha - resolver nome de usuário é só
   *  contexto de exibição, nunca deve derrubar a listagem de aditivos/medições/parcelas. */
  public List<UserSummary> fetchUsers(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }

    try {
      UserSummary[] result = restClient.get()
          .uri(uriBuilder -> uriBuilder.path("/internal/users").queryParam("ids", ids).build())
          .header("X-Internal-Secret", internalApiSecret)
          .retrieve()
          .body(UserSummary[].class);

      return result != null ? List.of(result) : List.of();
    } catch (Exception e) {
      log.warn("Falha ao resolver usuários no NimbusAuth: {}", e.getMessage());
      return List.of();
    }
  }

  /** Lista leve dos usuários vinculados a algum grupo do app_key informado (ver
   *  GET /internal/users/options no NimbusAuth) - autenticado só pelo secret compartilhado, não
   *  pelo token do usuário chamador, então não exige USERS_CONSULT (ver AdminUserService.options
   *  /AdminUserService.optionsFilter) nem expõe o diretório global de outros apps Nimbus (ao
   *  contrário de GET /api/v1/users/options, que é global de propósito). Não degrada
   *  silenciosamente (ao contrário de fetchUsers acima) - é a fonte de dados do seletor "escolha
   *  um usuário" de Chamados/Departamentos/Usuários, uma falha aqui deve aparecer como erro pro
   *  chamador, não uma lista vazia silenciosa que pareceria "nenhum usuário cadastrado". */
  public List<UserSummary> fetchOptionsByAppKey(String appKey) {
    UserSummary[] result = restClient.get()
        .uri(uriBuilder -> uriBuilder.path("/internal/users/options").queryParam("appKey", appKey).build())
        .header("X-Internal-Secret", internalApiSecret)
        .retrieve()
        .body(UserSummary[].class);

    return result != null ? List.of(result) : List.of();
  }

  /** Usuários (do app_key informado) que têm a permissão pedida (ver GET
   *  /internal/users/permissions no NimbusAuth) - usado por {@code VoucherScheduledTasks} pra
   *  resolver os destinatários do aviso de vouchers vencidos a partir de quem tem
   *  VOUCHER_NOTIFICATION, em vez de uma lista de e-mails configurada manualmente e sujeita a
   *  ficar desincronizada do catálogo real de usuários/grupos. Não degrada silenciosamente (mesmo
   *  critério de {@link #fetchOptionsByAppKey}) - o chamador decide como reagir a uma falha. */
  public List<UserSummary> fetchOptionsByPermission(String appKey, String permission) {
    UserSummary[] result = restClient.get()
        .uri(uriBuilder -> uriBuilder.path("/internal/users/permissions")
            .queryParam("appKey", appKey)
            .queryParam("permission", permission)
            .build())
        .header("X-Internal-Secret", internalApiSecret)
        .retrieve()
        .body(UserSummary[].class);

    return result != null ? List.of(result) : List.of();
  }
}
