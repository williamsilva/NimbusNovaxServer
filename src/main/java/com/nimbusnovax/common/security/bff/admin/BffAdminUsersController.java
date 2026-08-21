package com.nimbusnovax.common.security.bff.admin;

import com.nimbusnovax.common.security.BffAccessTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Menu "Segurança" &gt; Usuários - administração (não self-service, ver BffAccountController pra
 * "Minha conta"). Só usuários vinculados a algum grupo do NimbusNovax (ver AdminUserService.list) -
 * usuários são globais no NimbusAuth, mas o NimbusNovaxWeb não deve expor o diretório completo
 * (outros apps Nimbus, ex.: Cardsync).
 *
 * <p>Sem exclusão física (o NimbusAuth não tem esse endpoint - só ativar/desativar) e sem reset de
 * senha pelo admin (não existe no NimbusAuth - o único mecanismo é reenviar convite, que gera um
 * novo token de definição de senha pro próprio usuário).
 */
@RestController
@RequiredArgsConstructor
public class BffAdminUsersController {

  private final AdminUserService service;
  private final BffAccessTokenService accessTokenService;

  @GetMapping("/bff/v1/users")
  public List<AdminUserResponse> list(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.list(accessTokenService.getValidAccessToken(auth, request, response));
  }

  @GetMapping("/bff/v1/users/{id}")
  public AdminUserResponse get(
      @PathVariable UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.get(accessTokenService.getValidAccessToken(auth, request, response), id);
  }

  /** Sem accessToken/permissão - qualquer usuário autenticado do NimbusNovax pode ver este
   *  seletor leve (ver AdminUserService.options). */
  @GetMapping("/bff/v1/users/options")
  public List<AdminUserMinimalResponse> options() {
    return service.options();
  }

  /** Mesma lista de options() - usada pelo filtro "Cadastrado por" das telas de Usuários/Grupos. */
  @GetMapping("/bff/v1/users/options-filter")
  public List<AdminUserMinimalResponse> optionsFilter() {
    return service.options();
  }

  @PostMapping("/bff/v1/users/search")
  public AdminPageResponse<AdminUserResponse> search(
      @RequestBody AdminSearchRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.search(accessTokenService.getValidAccessToken(auth, request, response), body);
  }

  @PostMapping("/bff/v1/users")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminUserResponse create(
      @Valid @RequestBody AdminUserRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.create(accessTokenService.getValidAccessToken(auth, request, response), body);
  }

  @PutMapping("/bff/v1/users/{id}")
  public AdminUserResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody AdminUserRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.update(accessTokenService.getValidAccessToken(auth, request, response), id, body);
  }

  @PostMapping("/bff/v1/users/{id}/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activate(@PathVariable UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    service.activate(accessTokenService.getValidAccessToken(auth, request, response), id);
  }

  @PostMapping("/bff/v1/users/{id}/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivate(@PathVariable UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    service.deactivate(accessTokenService.getValidAccessToken(auth, request, response), id);
  }

  @PostMapping("/bff/v1/users/{id}/resend-invite")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resendInvite(@PathVariable UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    service.resendInvite(accessTokenService.getValidAccessToken(auth, request, response), id);
  }

  @PostMapping("/bff/v1/users/activate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void activateBulk(
      @Valid @RequestBody AdminBulkIdsRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    service.activateBulk(accessTokenService.getValidAccessToken(auth, request, response), body.ids());
  }

  @PostMapping("/bff/v1/users/deactivate")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deactivateBulk(
      @Valid @RequestBody AdminBulkIdsRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    service.deactivateBulk(accessTokenService.getValidAccessToken(auth, request, response), body.ids());
  }
}
