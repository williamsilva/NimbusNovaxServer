package com.nimbusnovax.common.company;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Serve a logo da empresa sem autenticação - consumida pelo cliente de e-mail de quem recebe o
 *  voucher e pelo openhtmltopdf ao gerar o PDF anexado (ver VoucherPdfService), nenhum dos dois
 *  tem sessão/cookie ou Bearer token. Caminho ({@link CompanySettingsService#LOGO_PATH}) fica de
 *  propósito fora de /api/** e /bff/** - não casa com nenhuma das duas security filter chains de
 *  {@code SecurityConfig}, então passa direto sem exigir autenticação (mesmo mecanismo dos assets
 *  estáticos de marca do NimbusAuth/CardsyncServer, ex.: /assets/cardsync-logo.png). */
@RestController
@RequiredArgsConstructor
public class PublicCompanyLogoController {

  private final CompanySettingsService service;

  @GetMapping(CompanySettingsService.LOGO_PATH)
  public ResponseEntity<byte[]> logo() {
    return service.findLogo()
        .map(logo -> ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(logo.contentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)))
            .body(logo.data()))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
