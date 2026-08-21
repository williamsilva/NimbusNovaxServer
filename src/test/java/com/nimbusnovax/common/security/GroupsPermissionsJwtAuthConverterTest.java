package com.nimbusnovax.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class GroupsPermissionsJwtAuthConverterTest {

  private final GroupsPermissionsJwtAuthConverter converter = new GroupsPermissionsJwtAuthConverter();

  @Test
  void mapsGroupsAndPermissionsToPrefixedAuthorities() {
    Jwt jwt = jwtWithClaims(Map.of(
        "sub", "user-1",
        "username", "gestor.teste",
        "groups", List.of("GESTOR_OBRAS"),
        "permissions", List.of("OBRAS_VIEW")));

    AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_GESTOR_OBRAS", "PERM_OBRAS_VIEW");
    assertThat(token.getName()).isEqualTo("gestor.teste");
  }

  @Test
  void fallsBackToSubjectWhenUsernameClaimIsAbsent() {
    Jwt jwt = jwtWithClaims(Map.of("sub", "user-1"));

    AbstractAuthenticationToken token = converter.convert(jwt);

    assertThat(token.getAuthorities()).isEmpty();
    assertThat(token.getName()).isEqualTo("user-1");
  }

  private Jwt jwtWithClaims(Map<String, Object> claims) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claims(c -> c.putAll(claims))
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .build();
  }
}
