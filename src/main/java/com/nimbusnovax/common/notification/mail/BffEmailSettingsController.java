package com.nimbusnovax.common.notification.mail;

import com.nimbusnovax.common.security.CurrentUserProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Menu "Configurações &gt; E-mail" - sessão/cookie (chain /bff/**, não JWT), mesmo padrão de
 * BffAdminUsersController. Permissões próprias do app nimbusnovax, seguem seedadas via migration
 * no repo NimbusAuth (EMAIL_SETTINGS_CONSULT/EMAIL_SETTINGS_PROCESS) - ver
 * com.nimbusnovax.common.security.CurrentUserProvider e o padrão já documentado na memória
 * "projeto-manage-permission-gap".
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bff/v1/email/settings")
public class BffEmailSettingsController {

  private final EmailSettingsService emailSettingsService;
  private final CurrentUserProvider currentUserProvider;

  @GetMapping
  public EmailSettingsModel getSettings() {
    requireAuthority("EMAIL_SETTINGS_CONSULT");
    return emailSettingsService.getSettings();
  }

  @PutMapping
  public EmailSettingsModel updateSettings(@Valid @RequestBody EmailSettingsRequest request) {
    requireAuthority("EMAIL_SETTINGS_PROCESS");
    return emailSettingsService.update(request);
  }

  /** @param permission nome cru (ex.: "EMAIL_SETTINGS_CONSULT") - authorities já vêm prefixadas
   *  "PERM_" (ver bffOidcUserService em SecurityConfig). */
  private void requireAuthority(String permission) {
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
    }
  }
}
