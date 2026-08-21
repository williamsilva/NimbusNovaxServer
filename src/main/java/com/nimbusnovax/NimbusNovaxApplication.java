package com.nimbusnovax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class NimbusNovaxApplication {

	public static void main(String[] args) {
		SpringApplication.run(NimbusNovaxApplication.class, args);
	}
}
