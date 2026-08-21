package com.nimbusnovax.common.notification.mail;

import com.nimbusnovax.common.security.BffAccessTokenService;
import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.web.PageResponse;
import com.nimbusnovax.common.web.SearchRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Menu "Configurações &gt; Auditoria de E-mail" - sessão/cookie (chain /bff/**, não JWT), mesmo
 * padrão de BffEmailSettingsController. Permissão própria do app nimbusnovax, seguem seedadas via
 * migration no repo NimbusAuth (EMAIL_LOG_CONSULT) - ver memória "projeto-manage-permission-gap".
 * Só leitura - não existe endpoint de reenvio nem de detalhe por id (o body já vem completo na
 * listagem, ver EmailLogModel.body). Além do próprio email_log, a busca também mescla convite/
 * reset de senha do NimbusAuth (ver EmailLogService.search) - por isso precisa do access token do
 * usuário logado, igual BffAdminGroupsController/BffAdminUsersController.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/bff/v1/email/logs")
public class BffEmailLogController {

  private final EmailLogService emailLogService;
  private final CurrentUserProvider currentUserProvider;
  private final BffAccessTokenService accessTokenService;

  @PostMapping("/search")
  public PageResponse<EmailLogModel> search(
      @RequestBody SearchRequest request,
      Authentication auth, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
    requireAuthority("EMAIL_LOG_CONSULT");
    String accessToken = accessTokenService.getValidAccessToken(auth, httpRequest, httpResponse);
    return emailLogService.search(request, accessToken);
  }

  /** @param permission nome cru (ex.: "EMAIL_LOG_CONSULT") - authorities já vêm prefixadas
   *  "PERM_" (ver bffOidcUserService em SecurityConfig). */
  private void requireAuthority(String permission) {
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
    }
  }
}
