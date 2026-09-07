package com.nimbusnovax;

import com.nimbusnovax.support.NimbusNovaxIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the full application context against a real Postgres+PostGIS container,
 * proving the Flyway migrations apply cleanly. nimbusAuth/storage values below
 * are placeholders only: the resource server and OAuth2 client both resolve
 * their remote endpoints lazily, so no real IdP is contacted on startup.
 */
@SpringBootTest
class NimbusNovaxApplicationTests extends NimbusNovaxIntegrationTestSupport {

	@Test
	void contextLoads() {
	}
}
