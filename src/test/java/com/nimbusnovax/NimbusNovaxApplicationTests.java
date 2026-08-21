package com.nimbusnovax;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Boots the full application context against a real Postgres+PostGIS container,
 * proving the Flyway migrations apply cleanly. nimbusAuth/storage values below
 * are placeholders only: the resource server and OAuth2 client both resolve
 * their remote endpoints lazily, so no real IdP is contacted on startup.
 */
@Testcontainers
@SpringBootTest
class NimbusNovaxApplicationTests {

	private static final DockerImageName POSTGIS_IMAGE = DockerImageName
			.parse("postgis/postgis:16-3.4-alpine")
			.asCompatibleSubstituteFor("postgres");

	@Container
	static PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(POSTGIS_IMAGE);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgis::getJdbcUrl);
		registry.add("spring.datasource.username", postgis::getUsername);
		registry.add("spring.datasource.password", postgis::getPassword);

		registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.provider", () -> "nimbusnovax-as");
		registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.client-id", () -> "nimbusnovax-bff");
		registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.client-secret", () -> "placeholder");
		registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.authorization-grant-type", () -> "authorization_code");
		registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.redirect-uri",
				() -> "{baseUrl}/login/oauth2/code/{registrationId}");
		registry.add("spring.security.oauth2.client.registration.nimbusnovax-bff.scope", () -> "openid,profile");
		registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.authorization-uri",
				() -> "https://nimbusauth.local.example/oauth2/authorize");
		registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.token-uri",
				() -> "https://nimbusauth.local.example/oauth2/token");
		registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.jwk-set-uri",
				() -> "https://nimbusauth.local.example/oauth2/jwks");
		registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.user-info-uri",
				() -> "https://nimbusauth.local.example/userinfo");
		registry.add("spring.security.oauth2.client.provider.nimbusnovax-as.user-name-attribute", () -> "sub");

		registry.add("nimbusnovax.security.issuer", () -> "https://nimbusauth.local.example");
		registry.add("nimbusnovax.security.cookies.secure", () -> "false");
		registry.add("nimbusnovax.security.cookies.same-site", () -> "Lax");
		registry.add("nimbusnovax.security.resource-server.jwk-set-uri", () -> "https://nimbusauth.local.example/oauth2/jwks");
		registry.add("nimbusnovax.security.web.spa-base-url", () -> "http://localhost:4201");
		registry.add("nimbusnovax.security.web.allowed-origins", () -> "http://localhost:4201");

		registry.add("storage.endpoint", () -> "http://minio.local.example:9000");
		registry.add("storage.access-key", () -> "placeholder");
		registry.add("storage.secret-key", () -> "placeholder");
	}

	@Test
	void contextLoads() {
	}
}
