package com.nimbusnovax.support;

import com.nimbussystems.commons.testsupport.AbstractPostgisContainerTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Container + datasource vêm de {@link AbstractPostgisContainerTest} (NimbusCommonsServer test
 * fixtures, extraído em 2026-09-07 - ver README do NimbusCommonsServer). Aqui só ficam as
 * propriedades específicas do NimbusNovaxServer (nomes de client/provider OAuth2, prefixo
 * nimbusnovax.security.*, storage.*).
 */
public abstract class NimbusNovaxIntegrationTestSupport extends AbstractPostgisContainerTest {

  @DynamicPropertySource
  static void registerNimbusNovaxProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.provider", () -> "nimbusnovax-as");
    registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.client-id", () -> "nimbusnovax-bff");
    registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.client-secret", () -> "placeholder");
    registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.authorization-grant-type", () -> "authorization_code");
    registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.redirect-uri",
        () -> "{baseUrl}/login/oauth2/code/{registrationId}");
    registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.scope", () -> "openid,profile");
    registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.authorization-uri",
        () -> "https://nimbusauth.local.example/oauth2/authorize");
    registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.token-uri",
        () -> "https://nimbusauth.local.example/oauth2/token");
    registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.jwk-set-uri",
        () -> "https://nimbusauth.local.example/oauth2/jwks");
    registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.user-info-uri",
        () -> "https://nimbusauth.local.example/userinfo");
    registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.user-name-attribute", () -> "sub");

    registry.add("nimbusnovax.security.issuer", () -> "https://nimbusauth.local.example");
    registry.add("nimbusnovax.security.cookies.secure", () -> "false");
    registry.add("nimbusnovax.security.cookies.same-site", () -> "Lax");
    registry.add("nimbusnovax.security.resource-server.jwk-set-uri", () -> "https://nimbusauth.local.example/oauth2/jwks");
    registry.add("nimbusnovax.security.web.spa-base-url", () -> "http://localhost:4201");
    registry.add("nimbusnovax.security.web.allowed-origins", () -> "http://localhost:4201");

    registry.add("storage.endpoint", () -> "http://minio.local.example:9000");
    registry.add("storage.access-key", () -> "placeholder");
    registry.add("storage.secret-key", () -> "placeholder");
  }
}
