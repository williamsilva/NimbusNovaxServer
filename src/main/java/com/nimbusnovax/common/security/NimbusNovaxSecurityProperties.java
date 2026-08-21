package com.nimbusnovax.common.security;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Config do BFF do NimbusNovax: não emite tokens nem faz login local — só valida JWTs emitidos
 * pelo NimbusAuth (issuer/JWKS remotos) e mantém sua própria sessão de BFF (cookies/CORS).
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "nimbusnovax.security")
public class NimbusNovaxSecurityProperties {

  /** Issuer do NimbusAuth (usado para validar o JWT e para resolver o JWKS remoto). */
  @NotNull
  private String issuer;

  @NotNull
  private Cookies cookies;

  private Web web = new Web();
  private ResourceServer resourceServer = new ResourceServer();

  @Data
  public static class ResourceServer {
    private boolean enabled = true;

    /**
     * URI do JWKS alcançável PELO BACKEND (não pelo browser) — pode divergir do `issuer` quando o
     * backend roda containerizado (docker compose) e o NimbusAuth roda nativamente no host: o
     * container não resolve "localhost" para o host, então precisa de host.docker.internal.
     */
    @NotNull
    private String jwkSetUri;
  }

  @Data
  public static class Web {
    /** Base URL do front-end (SPA) para redirecionamentos do lado servidor (ex: após login). */
    private String spaBaseUrl;
    private List<String> allowedOrigins;
  }

  @Data
  public static class Cookies {
    private String domain;
    private boolean secure;
    private String sameSite;
  }
}
