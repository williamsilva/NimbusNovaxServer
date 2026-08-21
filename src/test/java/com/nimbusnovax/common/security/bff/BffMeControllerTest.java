package com.nimbusnovax.common.security.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusnovax.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

class BffMeControllerTest {

  private final BffMeController controller = new BffMeController(new CurrentUserProvider());

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void returnsNotAuthenticatedWhenNoAuthentication() {
    HttpServletRequest request = mock(HttpServletRequest.class);

    BffMeController.MeResponse response = controller.me(null, request);

    assertThat(response.authenticated()).isFalse();
    assertThat(response.groups()).isEmpty();
  }

  @Test
  void returnsUserGroupsAndPermissionsForAuthenticatedSession() {
    OidcIdToken idToken = new OidcIdToken(
        "id-token-value",
        Instant.now(),
        Instant.now().plusSeconds(3600),
        Map.of("sub", "user-1", "userId", "42", "username", "gestor.teste", "name", "Gestor Teste"));

    DefaultOidcUser oidcUser = new DefaultOidcUser(
        List.of(new SimpleGrantedAuthority("ROLE_GESTOR_OBRAS"), new SimpleGrantedAuthority("PERM_OBRAS_VIEW")),
        idToken);

    OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "nimbusnovax-bff");
    SecurityContextHolder.getContext().setAuthentication(auth);

    HttpSession session = mock(HttpSession.class);
    when(session.getLastAccessedTime()).thenReturn(System.currentTimeMillis());
    when(session.getMaxInactiveInterval()).thenReturn(1800);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getSession(false)).thenReturn(session);

    BffMeController.MeResponse response = controller.me(auth, request);

    assertThat(response.authenticated()).isTrue();
    assertThat(response.userId()).isEqualTo("42");
    assertThat(response.username()).isEqualTo("gestor.teste");
    assertThat(response.name()).isEqualTo("Gestor Teste");
    assertThat(response.groups()).containsExactly("GESTOR_OBRAS");
    assertThat(response.permissions()).containsExactly("OBRAS_VIEW");
    assertThat(response.expiresAt()).isNotNull();
  }
}
