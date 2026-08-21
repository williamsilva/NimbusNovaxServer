package com.nimbusnovax.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusnovax.common.security.bff.ChangePasswordRequest;
import com.nimbusnovax.common.security.bff.ProfileResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.server.ResponseStatusException;

/**
 * @RestClientTest(NimbusAuthClient.class) autoconfigura o RestClient.Builder REAL do Spring Boot
 * (com o ObjectMapper leniente do próprio Boot - fail-on-unknown-properties=false por padrão, não
 * um builder cru) e constrói o NimbusAuthClient uma única vez via injeção de dependência -
 * importante chamar RestClient.Builder.build() só uma vez por classe de teste (o
 * MockServerRestClientCustomizer do Spring Boot lança IllegalStateException se detectar o
 * builder sendo construído mais de uma vez), então o client é @Autowired, não reconstruído em
 * cada teste.
 */
@RestClientTest(NimbusAuthClient.class)
@EnableConfigurationProperties(NimbusAuthProxyProperties.class)
@TestPropertySource(properties = "nimbusnovax.nimbusauth.base-url=http://nimbusauth.test")
class NimbusAuthClientTest {

  @Autowired
  private NimbusAuthClient client;

  @Autowired
  private MockRestServiceServer server;

  @Test
  void getMyProfileParsesResponseIgnoringFieldsNotDeclaredInProfileResponse() {
    server.expect(requestTo("http://nimbusauth.test/api/v1/me/profile"))
        .andExpect(header("Authorization", "Bearer tok-123"))
        .andRespond(withSuccess("""
            {
              "id": "3f2a9e10-8b7d-4e51-9c2a-1d0f6b7a44e2",
              "name": "Fiscal Teste",
              "userName": "fiscal@acquamania.com.br",
              "document": "12345678901",
              "status": 1,
              "createdAt": "2025-11-02T14:03:21.512Z",
              "lastLoginAt": "2026-08-05T13:10:04.221Z",
              "blockedUntil": null,
              "passwordExpiresAt": "2026-08-15T00:00:00Z",
              "createdBy": {"id": "a1b2c3d4-1111-1111-1111-111111111111", "name": "Suporte"},
              "groups": [{"id": "g-1", "name": "ADMIN", "description": "x", "appKey": "cardsync"}],
              "_links": {"self": {"href": "http://nimbusauth.test/api/v1/users/3f2a9e10-..."}}
            }
            """, MediaType.APPLICATION_JSON));

    ProfileResponse response = client.getMyProfile("tok-123");

    assertThat(response.name()).isEqualTo("Fiscal Teste");
    assertThat(response.userName()).isEqualTo("fiscal@acquamania.com.br");
    assertThat(response.document()).isEqualTo("12345678901");
    assertThat(response.status()).isEqualTo(1);
    assertThat(response.blockedUntil()).isNull();
  }

  @Test
  void mapsKnownNimbusAuthErrorCodeToBadRequestWithSameReason() {
    server.expect(requestTo("http://nimbusauth.test/api/v1/me/password/change"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"code\":\"PASSWORD_CURRENT_INVALID\",\"message\":\"Current password does not match\"}"));

    assertThatThrownBy(() -> client.changeMyPassword("tok-123", new ChangePasswordRequest("a", "b", "b")))
        .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
          assertThat(ex.getStatusCode().value()).isEqualTo(400);
          assertThat(ex.getReason()).isEqualTo("PASSWORD_CURRENT_INVALID");
        });
  }

  @Test
  void mapsUnexpectedUpstreamFailureToBadGatewayWithoutLeakingDetail() {
    server.expect(requestTo("http://nimbusauth.test/api/password/policy")).andRespond(withServerError());

    assertThatThrownBy(() -> client.getPasswordPolicy())
        .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
          assertThat(ex.getStatusCode().value()).isEqualTo(502);
          assertThat(ex.getReason()).isEqualTo("NIMBUS_AUTH_ERROR");
        });
  }

  @Test
  void badRequestWithoutParsableCodeStillMapsToBadGateway() {
    server.expect(requestTo("http://nimbusauth.test/api/password/policy/check"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("not json"));

    assertThatThrownBy(() -> client.checkPasswordPolicy("x", "x", "user"))
        .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode().value()).isEqualTo(502));
  }
}
