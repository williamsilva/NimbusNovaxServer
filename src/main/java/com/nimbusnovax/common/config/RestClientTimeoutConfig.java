package com.nimbusnovax.common.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

/**
 * Timeout padrão (conexão + leitura) pra todo `RestClient.Builder` auto-configurado do Spring
 * Boot - sem isto, uma lentidão/indisponibilidade momentânea de um serviço remoto (NimbusAuth,
 * etc.) trava a chamada até o timeout default do cliente (nenhum), travando a requisição HTTP
 * inteira que a originou. Só descoberto porque com.nimbusnovax.common.security.NimbusAuthClient
 * (perfil próprio via GET /api/v1/me/profile) nunca tinha sido exercitado em produção antes.
 *
 * <p>Um {@code @Bean RestClientCustomizer} (em vez de configurar o `requestFactory` direto dentro
 * de cada client, como já era feito em EmailSenderServiceRouter) porque testes de fatia
 * (`@RestClientTest`) já injetam seu próprio `MockServerRestClientCustomizer` no
 * `RestClient.Builder` pra interceptar chamadas via `MockRestServiceServer` - se o `requestFactory`
 * fosse setado manualmente dentro do construtor do client (como antes), ele sobrescreveria o do
 * mock (aplicado antes, na fábrica do bean `RestClient.Builder`) e quebraria esses testes com
 * tentativas de conexão de rede real. `@RestClientTest` é uma fatia de contexto que não faz
 * component scan da aplicação inteira, então esta classe (fora do slice) nunca é carregada nesses
 * testes - só a aplicação real usa este customizer, o mock nunca precisa disputar com ele.
 */
@Configuration
public class RestClientTimeoutConfig {

  @Bean
  public RestClientCustomizer restClientTimeoutCustomizer() {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(10));

    return builder -> builder.requestFactory(factory);
  }
}
