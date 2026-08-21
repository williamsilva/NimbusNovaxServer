package com.nimbusnovax.support;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test proving the Testcontainers + Postgres/PostGIS base setup works,
 * before any real spatial query is implemented (Fase 2+).
 */
@Testcontainers
class PostgisContainerTest {

	private static final DockerImageName POSTGIS_IMAGE = DockerImageName
			.parse("postgis/postgis:16-3.4-alpine")
			.asCompatibleSubstituteFor("postgres");

	@Container
	static PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(POSTGIS_IMAGE);

	@Test
	void startsPostgisContainerAndAcceptsConnections() {
		assertThat(postgis.isRunning()).isTrue();
		assertThat(postgis.getJdbcUrl()).contains("jdbc:postgresql");
	}
}
