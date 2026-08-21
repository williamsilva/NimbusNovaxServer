package com.nimbusnovax.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

/**
 * Validação de audience (aud) foi deixada de fora por ora: não há confirmação (via um token real
 * do NimbusAuth) de que ele emite um claim `aud` distinto por client - só issuer + assinatura
 * (JWKS) são validados aqui, o que já é a garantia de segurança principal (só o NimbusAuth possui
 * a chave privada para assinar tokens com esse issuer). Reavaliar quando isso for confirmado.
 */
@Configuration
@RequiredArgsConstructor
public class ResourceServerJwtBeans {

  private final NimbusNovaxSecurityProperties props;

  /**
   * withJwkSetUri (carregamento preguiçoso, só na primeira validação de token) em vez de
   * withIssuerLocation (faria discovery via rede aqui mesmo, no boot do NimbusNovax, exigindo que
   * o NimbusAuth já esteja no ar nesse instante). URI vem de resource-server.jwk-set-uri (não de
   * `issuer` diretamente) porque pode divergir dele — ver javadoc de NimbusNovaxSecurityProperties.ResourceServer.
   */
  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withJwkSetUri(props.getResourceServer().getJwkSetUri()).build();
  }

  @Bean
  public JwtAuthenticationConverterAdapter jwtAuthenticationConverterAdapter() {
    return new JwtAuthenticationConverterAdapter(new GroupsPermissionsJwtAuthConverter());
  }

  @Bean
  public OAuth2TokenValidator<Jwt> jwtTokenValidator() {
    return JwtValidators.createDefaultWithIssuer(props.getIssuer());
  }

  /** Adapter simples para plugar nosso Converter<Jwt, AbstractAuthenticationToken> no ponto esperado pelo Spring Security. */
  public static class JwtAuthenticationConverterAdapter extends JwtAuthenticationConverter {
    public JwtAuthenticationConverterAdapter(GroupsPermissionsJwtAuthConverter converter) {
      setJwtGrantedAuthoritiesConverter(jwt -> converter.convert(jwt).getAuthorities());
      setPrincipalClaimName("username");
    }
  }
}
