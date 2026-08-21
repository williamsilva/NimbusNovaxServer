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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Menu "Segurança" &gt; Grupos - administração, escopada a grupos/permissões do NimbusNovax. */
@RestController
@RequiredArgsConstructor
public class BffAdminGroupsController {

  private final AdminGroupService service;
  private final BffAccessTokenService accessTokenService;

  @GetMapping("/bff/v1/groups")
  public List<AdminGroupSummaryResponse> list(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.list(accessTokenService.getValidAccessToken(auth, request, response));
  }

  @GetMapping("/bff/v1/groups/options")
  public List<AdminGroupOptionResponse> options(
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.options(accessTokenService.getValidAccessToken(auth, request, response));
  }

  @PostMapping("/bff/v1/groups/search")
  public AdminPageResponse<AdminGroupSummaryResponse> search(
      @RequestBody AdminSearchRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.search(accessTokenService.getValidAccessToken(auth, request, response), body);
  }

  @GetMapping("/bff/v1/groups/{id}")
  public AdminGroupResponse get(@PathVariable UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.get(accessTokenService.getValidAccessToken(auth, request, response), id);
  }

  @PostMapping("/bff/v1/groups")
  @ResponseStatus(HttpStatus.CREATED)
  public AdminGroupResponse create(
      @Valid @RequestBody AdminGroupRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.create(accessTokenService.getValidAccessToken(auth, request, response), body);
  }

  @PutMapping("/bff/v1/groups/{id}")
  public AdminGroupResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody AdminGroupRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.update(accessTokenService.getValidAccessToken(auth, request, response), id, body);
  }

  @DeleteMapping("/bff/v1/groups/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    service.delete(accessTokenService.getValidAccessToken(auth, request, response), id);
  }

  @PutMapping("/bff/v1/groups/{id}/permissions")
  public AdminGroupResponse updatePermissions(
      @PathVariable UUID id,
      @Valid @RequestBody AdminGroupPermissionsRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.updatePermissions(accessTokenService.getValidAccessToken(auth, request, response), id, body);
  }

  @PutMapping("/bff/v1/groups/{id}/users")
  public AdminGroupResponse updateUsers(
      @PathVariable UUID id,
      @Valid @RequestBody AdminGroupUsersRequest body,
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.updateUsers(accessTokenService.getValidAccessToken(auth, request, response), id, body);
  }

  @GetMapping({"/bff/v1/permissions", "/bff/v1/permissions/options"})
  public List<AdminPermissionOptionResponse> listPermissionOptions(
      Authentication auth, HttpServletRequest request, HttpServletResponse response) {
    return service.listPermissionOptions(accessTokenService.getValidAccessToken(auth, request, response));
  }
}
