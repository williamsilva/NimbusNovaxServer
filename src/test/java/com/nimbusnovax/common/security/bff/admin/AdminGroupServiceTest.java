package com.nimbusnovax.common.security.bff.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.common.security.NimbusAuthAdminClient;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawGroup;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawPermissionOption;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawUserMinimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AdminGroupServiceTest {

  private static final String TOKEN = "token";

  private final NimbusAuthAdminClient client = mock(NimbusAuthAdminClient.class);
  private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
  private final AdminGroupService service = new AdminGroupService(client, currentUserProvider);

  @BeforeEach
  void setUp() {
    when(currentUserProvider.hasAuthority(any())).thenReturn(true);
  }

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
  void listRejectsWithoutConsultAuthority() {
    when(currentUserProvider.hasAuthority("PERM_GROUPS_CONSULT")).thenReturn(false);

    assertThatThrownBy(() -> service.list(TOKEN)).isInstanceOf(ResponseStatusException.class);
    verify(client, never()).searchAllGroups(any());
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

  @Test
  void createRejectsWithoutCreateAuthority() {
    when(currentUserProvider.hasAuthority("PERM_GROUPS_CREATE")).thenReturn(false);

    assertThatThrownBy(() -> service.create(TOKEN, new AdminGroupRequest("FISCAL", "Fiscal de obras")))
        .isInstanceOf(ResponseStatusException.class);
    verify(client, never()).createGroup(any(), any());
  }

  @Test
  void deleteRejectsWithoutDeleteAuthority() {
    when(currentUserProvider.hasAuthority("PERM_GROUPS_DELETE")).thenReturn(false);
    UUID groupId = UUID.randomUUID();

    assertThatThrownBy(() -> service.delete(TOKEN, groupId)).isInstanceOf(ResponseStatusException.class);
    verify(client, never()).deleteGroup(any(), any());
  }

  @Test
  void updatePermissionsRejectsWithoutManagementPermissionAuthority() {
    when(currentUserProvider.hasAuthority("PERM_GROUPS_MANAGEMENT_PERMISSION")).thenReturn(false);
    UUID groupId = UUID.randomUUID();

    assertThatThrownBy(() -> service.updatePermissions(TOKEN, groupId, new AdminGroupPermissionsRequest(List.of())))
        .isInstanceOf(ResponseStatusException.class);
    verify(client, never()).updateGroupPermissions(any(), any(), any());
  }

  @Test
  void updateUsersRejectsWithoutManagementUserAuthority() {
    when(currentUserProvider.hasAuthority("PERM_GROUPS_MANAGEMENT_USER")).thenReturn(false);
    UUID groupId = UUID.randomUUID();

    assertThatThrownBy(() -> service.updateUsers(TOKEN, groupId, new AdminGroupUsersRequest(List.of())))
        .isInstanceOf(ResponseStatusException.class);
    verify(client, never()).updateGroupUsers(any(), any(), any());
  }
}
