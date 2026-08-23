plugins {
	java
	id("org.springframework.boot") version "3.3.2"
	id("io.spring.dependency-management") version "1.1.6"
}

group = "com.nimbusnovax"
version = "0.1.0-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

val testcontainersVersion = "1.21.3"
val hibernateSpatialVersion = "6.5.2.Final"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-hateoas")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.session:spring-session-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")

	implementation("org.hibernate.orm:hibernate-spatial:$hibernateSpatialVersion")

	// Fase 7: @Auditable (com.nimbusnovax.common.audit) via Spring AOP.
	implementation("org.springframework.boot:spring-boot-starter-aop")

	// com.nimbusnovax.common.notification.mail - config de e-mail (FAKE/SMTP/API_KEY), mesmas
	// starters usadas pelo NimbusAuth/CardsyncServer pra isso.
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// com.nimbusnovax.voucher - geração de PDF (visualizar/enviar voucher) a partir de HTML/
	// Thymeleaf. O sistema legado usava JasperReports (.jrxml); optamos por HTML->PDF aqui por ser
	// consideravelmente mais simples de manter e não exigir compilar templates binários.
	implementation("com.openhtmltopdf:openhtmltopdf-pdfbox:1.0.10")

	runtimeOnly("org.postgresql:postgresql")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
	testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
