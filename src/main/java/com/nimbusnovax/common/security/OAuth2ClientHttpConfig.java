package com.nimbusnovax.common.security;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Timeout de conexão/leitura na renovação (refresh_token) do access_token OAuth2 usado pra
 * encaminhar como Bearer nas chamadas server-to-server pro NimbusAuth (ver
 * BffAccessTokenService/NimbusAuthClient) - sem isto, uma lentidão momentânea do NimbusAuth trava
 * a renovação até o gateway do Railway matar a conexão (504), mesmo depois do timeout já
 * adicionado em RestClientTimeoutConfig, porque a renovação de token é resolvida pelo
 * OAuth2AuthorizedClientManager autoconfigurado do Spring Security - um {@code RestTemplate}
 * interno próprio, separado do {@code RestClient.Builder} usado pelo resto da aplicação, então o
 * RestClientCustomizer daquela classe nunca chega a valer aqui.
 *
 * <p>Reconstrói manualmente a mesma configuração padrão que
 * {@link DefaultRefreshTokenTokenResponseClient} monta sozinho (os 2 message converters + o
 * error handler), só trocando o {@code ClientHttpRequestFactory} - não há setter mais direto
 * pra só o timeout nessa API do Spring Security.
 */
@Configuration
public class OAuth2ClientHttpConfig {

  @Bean
  public DefaultOAuth2AuthorizedClientManager authorizedClientManager(
      ClientRegistrationRepository clientRegistrationRepository,
      OAuth2AuthorizedClientRepository authorizedClientRepository) {

    OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
        .authorizationCode()
        .refreshToken(configurer -> configurer.accessTokenResponseClient(refreshTokenResponseClient()))
        .build();

    DefaultOAuth2AuthorizedClientManager manager =
        new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
    manager.setAuthorizedClientProvider(authorizedClientProvider);
    return manager;
  }

  private OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient() {
    DefaultRefreshTokenTokenResponseClient client = new DefaultRefreshTokenTokenResponseClient();
    client.setRestOperations(oauth2RestTemplate());
    return client;
  }

  private RestTemplate oauth2RestTemplate() {
    RestTemplate restTemplate = new RestTemplate(httpTimeouts());
    restTemplate.setMessageConverters(
        List.of(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
    restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
    return restTemplate;
  }

  private JdkClientHttpRequestFactory httpTimeouts() {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(10));
    return factory;
  }
}
