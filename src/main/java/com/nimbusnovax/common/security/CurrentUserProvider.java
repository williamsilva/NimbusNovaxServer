package com.nimbusnovax.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/**
 * Identidade do usuário autenticado na sessão do BFF (claims do id_token do nimbusAuth) — ponto
 * único de acesso reaproveitado por qualquer módulo que precise saber "quem está fazendo isso"
 * (ex.: preencher o dono de uma obra), evitando duplicar a extração feita em BffMeController.
 */
@Component
public class CurrentUserProvider {

  public record CurrentUser(String userId, String username, String name) {
  }

  public CurrentUser getCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth != null ? auth.getName() : null;
    String userId = null;
    String name = username;

    if (auth != null && auth.getPrincipal() instanceof OidcUser oidc) {
      OidcIdToken idToken = oidc.getIdToken();
      if (idToken != null) {
        Object userIdRaw = idToken.getClaim("userId");
        if (userIdRaw != null) {
          userId = String.valueOf(userIdRaw);
        }
        String nameClaim = idToken.getClaimAsString("name");
        if (nameClaim != null && !nameClaim.isBlank()) {
          name = nameClaim;
        }
        String usernameClaim = idToken.getClaimAsString("username");
        if (usernameClaim != null && !usernameClaim.isBlank()) {
          username = usernameClaim;
        }
      }
    }

    return new CurrentUser(userId, username, name);
  }

  /** @throws IllegalStateException se não houver usuário autenticado no contexto atual. */
  public String requireUserId() {
    String userId = getCurrentUser().userId();
    if (userId == null) {
      throw new IllegalStateException("No authenticated user in the current security context");
    }
    return userId;
  }

  /** Grupo "SUPPORT" (app_key='nimbusnovax', ver NimbusAuth) - vira essa authority via groups -> ROLE_ em bffOidcUserService. */
  private static final String ROLE_SUPPORT = "ROLE_SUPPORT";

  /**
   * Ex.: hasAuthority("PERM_ADITIVO_APPROVE_TIER1") — authorities já vêm prefixadas (ver
   * bffOidcUserService). Quem tem ROLE_SUPPORT passa em qualquer authority pedida - mesmo bypass
   * de CsDefaultSecurityMethod.hasAuthority no CardsyncServer, pro grupo SUPPORT (oculto da
   * interface, protegido contra edição/exclusão no próprio NimbusAuth) ter acesso irrestrito sem
   * precisar manter a lista de permissões em dia a cada entidade nova.
   */
  public boolean hasAuthority(String authority) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return false;
    }
    for (GrantedAuthority granted : auth.getAuthorities()) {
      String grantedAuthority = granted.getAuthority();
      if (ROLE_SUPPORT.equals(grantedAuthority) || authority.equals(grantedAuthority)) {
        return true;
      }
    }
    return false;
  }
}
