package com.nimbusnovax.common.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * com.nimbussystems.commons (NimbusCommonsServer) vive fora da árvore com.nimbusnovax, então o
 * component-scan/entity-scan/repository-scan default do Spring Boot (que só cobre o pacote da
 * classe @SpringBootApplication e sub-pacotes) não acha os @Service/@Entity/@Repository de lá
 * sozinho - precisa listar os dois pacotes explicitamente.
 *
 * <p>Isolado numa classe @Configuration própria (em vez de direto em NimbusNovaxApplication) de
 * propósito: um teste de fatia (@RestClientTest/@WebMvcTest/etc.) resolve a config raiz a partir
 * de NimbusNovaxApplication e filtra @Configuration/@Component não relevantes pro slice - mas
 * @EnableJpaRepositories/@EntityScan são processados por um ImportBeanDefinitionRegistrar que
 * ignora esse filtro quando declarado direto na classe @SpringBootApplication, registrando (e o
 * refresh do contexto tentando inicializar) os repositórios JPA mesmo em slices sem
 * EntityManagerFactory nenhum (mesmo achado do NimbusFlowServer, rodando NimbusAuthClientTest de
 * verdade - "No bean named 'entityManagerFactory' available"). Numa classe separada, o filtro de
 * slice exclui esta @Configuration como excluiria qualquer @Service comum.
 *
 * <p>Os dois pacotes (com.nimbusnovax + com.nimbussystems.commons) precisam estar juntos na MESMA
 * anotação @EntityScan/@EnableJpaRepositories - um @EntityScan(basePackages=only-commons) não
 * SOMA ao scan default do Spring Boot (que cobre com.nimbusnovax sozinho), ele SUBSTITUI.
 */
@Configuration
@ComponentScan(basePackages = "com.nimbussystems.commons")
@EntityScan(basePackages = {"com.nimbusnovax", "com.nimbussystems.commons"})
@EnableJpaRepositories(basePackages = {"com.nimbusnovax", "com.nimbussystems.commons"})
public class CommonsIntegrationConfig {
}
