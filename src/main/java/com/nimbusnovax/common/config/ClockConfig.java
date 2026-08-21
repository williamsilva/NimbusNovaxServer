package com.nimbusnovax.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Clock injetável (em vez de OffsetDateTime.now() direto) - permite testar UserDirectoryService
 *  (staleness do cache) sem depender do relógio real. */
@Configuration
public class ClockConfig {

  @Bean
  public Clock clockUtc() {
    return Clock.systemUTC();
  }
}
