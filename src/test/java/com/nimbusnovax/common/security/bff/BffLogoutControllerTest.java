package com.nimbusnovax.common.security.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusnovax.common.security.NimbusNovaxSecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.client.RestClient;

class BffLogoutControllerTest {

  private final NimbusNovaxSecurityProperties props = new NimbusNovaxSecurityProperties();
  private final OAuth2AuthorizedClientRepository authorizedClientRepository = mock(OAuth2AuthorizedClientRepository.class);
  // Não exercido pelos 2 testes abaixo (nenhum cobre o caminho de refresh_token que usa o
  // RestClient.Builder) - mock só pra satisfazer o construtor (ver fetchFreshIdToken).
  private final RestClient.Builder restClientBuilder = mock(RestClient.Builder.class);
  private final BffLogoutController controller =
      new BffLogoutController(props, authorizedClientRepository, restClientBuilder);

  {
    props.setIssuer("http://localhost:9090");
    NimbusNovaxSecurityProperties.Cookies cookies = new NimbusNovaxSecurityProperties.Cookies();
    cookies.setSecure(false);
    cookies.setSameSite("Lax");
    props.setCookies(cookies);
    props.getWeb().setSpaBaseUrl("http://localhost:4201");
  }

  @Test
  void buildsRpInitiatedLogoutUrlWhenOidcUserPresent() {
    OidcIdToken idToken = new OidcIdToken(
        "id-token-value", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "user-1"));
    DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(), idToken);
    OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "nimbusnovax-bff");

    HttpSession session = mock(HttpSession.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(session);
    HttpServletResponse response = mock(HttpServletResponse.class);

    var result = controller.logout(auth, request, response);

    assertThat(result.getBody().logoutUrl())
        .startsWith("http://localhost:9090/connect/logout?id_token_hint=id-token-value")
        .contains("post_logout_redirect_uri=http%3A%2F%2Flocalhost%3A4201");
    verify(session).invalidate();
    verify(authorizedClientRepository).removeAuthorizedClient(anyString(), any(), any(), any());
    verify(response, org.mockito.Mockito.times(2)).addHeader(org.mockito.ArgumentMatchers.eq("Set-Cookie"), anyString());
  }

  @Test
  void fallsBackToSpaBaseUrlWhenNotAuthenticated() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    var result = controller.logout(null, request, response);

    assertThat(result.getBody().logoutUrl()).isEqualTo("http://localhost:4201");
  }
}
