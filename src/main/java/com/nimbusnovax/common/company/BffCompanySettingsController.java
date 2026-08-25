package com.nimbusnovax.common.company;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Menu "Configurações &gt; Empresa" - mesmo padrão de {@code BffEmailSettingsController}.
 *  Permissões próprias do app nimbusnovax, seguem seedadas via migration no repo NimbusAuth
 *  (COMPANY_SETTINGS_CONSULT/COMPANY_SETTINGS_PROCESS). Endpoints de logo ficam aqui (autenticados,
 *  mesma permissão de PROCESS) - a leitura pública da imagem em si é servida à parte, por
 *  {@link PublicCompanyLogoController}, fora de /bff/**. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bff/v1/company-settings")
public class BffCompanySettingsController {

  private final CompanySettingsService service;

  @GetMapping
  public CompanySettingsModel getSettings() {
    return service.getSettings();
  }

  @PutMapping
  public CompanySettingsModel updateSettings(@Valid @RequestBody CompanySettingsRequest request) {
    return service.update(request);
  }

  @PostMapping("/logo")
  public CompanySettingsModel uploadLogo(@RequestParam("file") MultipartFile file) {
    return service.updateLogo(file);
  }

  @DeleteMapping("/logo")
  public CompanySettingsModel deleteLogo() {
    return service.deleteLogo();
  }
}
