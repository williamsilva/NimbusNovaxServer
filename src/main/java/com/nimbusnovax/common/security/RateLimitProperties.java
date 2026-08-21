package com.nimbusnovax.common.security;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Rate limiting básico por IP (PROJECT_SPEC.md seção 8, Fase 7: "rate limiting básico"). Sem
 * limites reais definidos no spec - valores abaixo são placeholder documentado como suposição,
 * generosos o bastante pro uso normal da UI (várias chamadas em paralelo ao abrir uma tela) mas
 * suficientes pra conter abuso/scraping grosseiro.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "nimbusnovax.rate-limit")
public class RateLimitProperties {

  private boolean enabled = true;

  @Min(1)
  private int capacity = 120;

  @Min(1)
  private int windowSeconds = 60;
}
