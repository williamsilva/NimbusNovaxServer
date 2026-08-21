package com.nimbusnovax.common.security.bff;

import com.nimbusnovax.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BffMeController {

  private final CurrentUserProvider currentUserProvider;

  public record MeResponse(
      boolean authenticated,
      String iss,
      String userId,
      String username,
      String name,
      List<String> groups,
      List<String> permissions,
      Instant expiresAt) {
  }

  @GetMapping("/bff/me")
  public MeResponse me(Authentication auth, HttpServletRequest request) {
    boolean authenticated = auth != null && auth.isAuthenticated();

    List<String> raw = auth == null
        ? List.of()
        : auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    List<String> groups = raw.stream()
        .filter(a -> a != null && a.startsWith("ROLE_"))
        .map(a -> a.substring("ROLE_".length()))
        .distinct()
        .toList();

    List<String> permissions = raw.stream()
        .filter(a -> a != null && a.startsWith("PERM_"))
        .map(a -> a.substring("PERM_".length()))
        .distinct()
        .toList();

    CurrentUserProvider.CurrentUser currentUser = currentUserProvider.getCurrentUser();

    String iss = null;
    if (auth != null && auth.getPrincipal() instanceof OidcUser oidc
        && oidc.getIdToken() != null && oidc.getIdToken().getIssuer() != null) {
      iss = oidc.getIdToken().getIssuer().toString();
    }

    // Expiração reportada ao SPA é a da sessão HTTP do BFF (renovada a cada request), não a do
    // id_token (fixa desde o login) - senão o "renew" do front nunca avança.
    Instant expiresAt = null;
    HttpSession session = request.getSession(false);
    if (authenticated && session != null) {
      expiresAt = Instant.ofEpochMilli(session.getLastAccessedTime()).plusSeconds(session.getMaxInactiveInterval());
    }

    return new MeResponse(
        authenticated, iss, currentUser.userId(), currentUser.username(), currentUser.name(), groups, permissions, expiresAt);
  }
}
