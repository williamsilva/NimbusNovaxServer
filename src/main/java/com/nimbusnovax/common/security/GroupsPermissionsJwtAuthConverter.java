package com.nimbusnovax.common.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Mapeia os claims flat que o NimbusAuth emite no access_token — "groups" -> ROLE_*,
 * "permissions" -> PERM_* — não os claims de realm do Keycloak (o NimbusAuth é um Spring
 * Authorization Server próprio, não Keycloak). Não usa scope/scp.
 */
public class GroupsPermissionsJwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();

    authorities.addAll(readStringList(jwt, "groups").stream().map(this::toRole).toList());
    authorities.addAll(readStringList(jwt, "permissions").stream().map(this::toPerm).toList());

    String name = jwt.getClaimAsString("username");
    if (name == null || name.isBlank()) {
      name = jwt.getSubject();
    }

    return new JwtAuthenticationToken(jwt, authorities, name);
  }

  private SimpleGrantedAuthority toRole(String group) {
    String g = group == null ? "" : group.trim();
    return new SimpleGrantedAuthority(g.startsWith("ROLE_") ? g : "ROLE_" + g);
  }

  private SimpleGrantedAuthority toPerm(String perm) {
    String p = perm == null ? "" : perm.trim();
    return new SimpleGrantedAuthority(p.startsWith("PERM_") ? p : "PERM_" + p);
  }

  private List<String> readStringList(Jwt jwt, String claim) {
    Object raw = jwt.getClaims().get(claim);
    if (raw instanceof Collection<?> c) {
      List<String> out = new ArrayList<>();
      for (Object o : c) {
        if (o != null) {
          out.add(o.toString());
        }
      }
      return out;
    }
    return List.of();
  }
}
