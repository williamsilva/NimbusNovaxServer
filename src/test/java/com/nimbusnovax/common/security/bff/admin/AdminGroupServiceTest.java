package com.nimbusnovax.common.security.bff.admin;

import com.nimbussystems.commons.security.bff.admin.AdminUserRequest;

import com.nimbussystems.commons.security.bff.admin.AdminUserMinimalResponse;

import com.nimbussystems.commons.security.bff.admin.AdminSearchRequest;

import com.nimbussystems.commons.security.bff.admin.AdminPermissionOptionResponse;

import com.nimbussystems.commons.security.bff.admin.AdminPageResponse;

import com.nimbussystems.commons.security.bff.admin.AdminGroupUsersRequest;

import com.nimbussystems.commons.security.bff.admin.AdminGroupSummaryResponse;

import com.nimbussystems.commons.security.bff.admin.AdminGroupResponse;

import com.nimbussystems.commons.security.bff.admin.AdminGroupRequest;

import com.nimbussystems.commons.security.bff.admin.AdminGroupPermissionsRequest;

import com.nimbussystems.commons.security.bff.admin.AdminGroupOptionResponse;

import com.nimbussystems.commons.security.bff.admin.AdminFilterSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusnovax.common.security.NimbusAuthAdminClient;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawGroup;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawPermissionOption;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawUserMinimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Sem checagem de permissão aqui - AdminGroupService não tem mais autorização manual
 *  (CurrentUserProvider foi removido do construtor), ela migrou pra {@code @CheckSecurity} nos
 *  métodos de {@code BffAdminGroupsController} (defesa aplicada via proxy AOP do Spring Security,
 *  não observável instanciando o service direto com "new" como este teste faz - cobrir a rejeição
 *  em si exigiria um teste de integração contra o controller, não existe nenhum ainda pra nenhum
 *  controller admin deste projeto). */
class AdminGroupServiceTest {

  private static final String TOKEN = "token";

  private final NimbusAuthAdminClient client = mock(NimbusAuthAdminClient.class);
  private final AdminGroupService service = new AdminGroupService(client);

  @Test
  void listMapsGroupSummaryWithCountsAndCreatedBy() {
    RawUserMinimal createdBy = new RawUserMinimal(UUID.randomUUID(), "William Silva", "william@acquamania.com.br");
    when(client.searchAllGroups(TOKEN)).thenReturn(
        List.of(new RawGroup(UUID.randomUUID(), "GESTOR", "Gestor de obras", 2, 1, null, createdBy, List.of(), List.of())));

    List<AdminGroupSummaryResponse> result = service.list(TOKEN);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("GESTOR");
    assertThat(result.get(0).usersCount()).isEqualTo(2);
    assertThat(result.get(0).permissionsCount()).isEqualTo(1);
    assertThat(result.get(0).createdBy().name()).isEqualTo("William Silva");
  }

  @Test
  void getMapsFullGroupWithPermissions() {
    UUID groupId = UUID.randomUUID();
    RawGroup raw = new RawGroup(groupId, "ADMINISTRADOR", "Administrador", 3, 2, null, null,
        List.of(new RawPermissionOption(UUID.randomUUID(), "USERS_CONSULT", "Consulta usuários", "nimbusnovax")),
        List.of());
    when(client.getGroup(TOKEN, groupId)).thenReturn(raw);

    AdminGroupResponse result = service.get(TOKEN, groupId);

    assertThat(result.name()).isEqualTo("ADMINISTRADOR");
    assertThat(result.permissions()).extracting(AdminPermissionOptionResponse::name).containsExactly("USERS_CONSULT");
  }

  @Test
  void getMapsFullGroupWithUsers() {
    UUID groupId = UUID.randomUUID();
    RawGroup raw = new RawGroup(groupId, "ADMINISTRADOR", "Administrador", 1, 0, null, null, List.of(),
        List.of(new RawUserMinimal(UUID.randomUUID(), "William Silva", "william@acquamania.com.br")));
    when(client.getGroup(TOKEN, groupId)).thenReturn(raw);

    AdminGroupResponse result = service.get(TOKEN, groupId);

    assertThat(result.users()).extracting(AdminUserMinimalResponse::name).containsExactly("William Silva");
  }
}
