package com.nimbusnovax.common.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Base URL do NimbusAuth alcançável PELO BACKEND (mesma URL usada em
 * NimbusNovaxSecurityProperties.ResourceServer.jwkSetUri, só que exposta separadamente aqui pra
 * ser reaproveitada nas chamadas REST de perfil/senha, sem depender do path fixo "/oauth2/jwks").
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "nimbusnovax.nimbusauth")
public class NimbusAuthProxyProperties {

  @NotBlank
  private String baseUrl;

  /** Secret compartilhado (header X-Internal-Secret) pra chamar a API interna machine-to-machine
   *  do NimbusAuth (/internal/users, ver NimbusAuthInternalClient) - mesmo valor já configurado
   *  no NimbusAuth e no CardsyncServer (NIMBUS_INTERNAL_API_SECRET), não é exclusivo do NimbusNovax. */
  @NotBlank
  private String internalApiSecret;
}
