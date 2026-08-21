package com.nimbusnovax.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserProviderTest {

  private final CurrentUserProvider provider = new CurrentUserProvider();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void hasAuthorityMatchesExactGrantedAuthority() {
    authenticateWith("PERM_OBRA_MANAGE");

    assertThat(provider.hasAuthority("PERM_OBRA_MANAGE")).isTrue();
    assertThat(provider.hasAuthority("PERM_PROJETO_MANAGE")).isFalse();
  }

  @Test
  void hasAuthorityIsFalseWithoutAuthentication() {
    SecurityContextHolder.clearContext();

    assertThat(provider.hasAuthority("PERM_OBRA_MANAGE")).isFalse();
  }

  @Test
  void roleSupportBypassesAnyRequestedAuthority() {
    authenticateWith("ROLE_SUPPORT");

    assertThat(provider.hasAuthority("PERM_OBRA_MANAGE")).isTrue();
    assertThat(provider.hasAuthority("PERM_PROJETO_MANAGE")).isTrue();
    assertThat(provider.hasAuthority("qualquer coisa - nem precisa existir")).isTrue();
  }

  @Test
  void regularUserWithoutRoleSupportStillNeedsTheExactAuthority() {
    authenticateWith("PERM_FORNECEDOR_MANAGE");

    assertThat(provider.hasAuthority("PERM_OBRA_MANAGE")).isFalse();
  }

  private void authenticateWith(String... authorities) {
    var grantedAuthorities = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
    TestingAuthenticationToken auth = new TestingAuthenticationToken("user-1", null, grantedAuthorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
