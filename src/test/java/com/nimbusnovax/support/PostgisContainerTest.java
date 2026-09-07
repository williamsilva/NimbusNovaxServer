package com.nimbusnovax.support;

import com.nimbussystems.commons.testsupport.AbstractPostgisContainerTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test proving the Testcontainers + Postgres/PostGIS base setup works,
 * before any real spatial query is implemented (Fase 2+).
 */
class PostgisContainerTest extends AbstractPostgisContainerTest {

	@Test
	void startsPostgisContainerAndAcceptsConnections() {
		assertThat(postgis.isRunning()).isTrue();
		assertThat(postgis.getJdbcUrl()).contains("jdbc:postgresql");
	}
}
