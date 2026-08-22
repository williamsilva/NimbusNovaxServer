package com.nimbusnovax.common.company;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Menu "Configurações &gt; Empresa" - mesmo padrão de {@code BffEmailSettingsController}.
 *  Permissões próprias do app nimbusnovax, seguem seedadas via migration no repo NimbusAuth
 *  (COMPANY_SETTINGS_CONSULT/COMPANY_SETTINGS_PROCESS). */
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
}
