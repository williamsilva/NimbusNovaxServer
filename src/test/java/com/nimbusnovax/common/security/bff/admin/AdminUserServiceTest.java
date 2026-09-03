package com.nimbusnovax.common.security.bff.admin;

import com.nimbussystems.commons.security.bff.admin.AdminUserResponse;

import com.nimbussystems.commons.security.bff.admin.AdminUserRequest;

import com.nimbussystems.commons.security.bff.admin.AdminUserMinimalResponse;

import com.nimbussystems.commons.security.bff.admin.AdminSearchRequest;

import com.nimbussystems.commons.security.bff.admin.AdminPageResponse;

import com.nimbussystems.commons.security.bff.admin.AdminGroupOptionResponse;

import com.nimbussystems.commons.security.bff.admin.AdminFilterSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusnovax.common.security.NimbusAuthAdminClient;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawGroupOption;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawUser;
import com.nimbusnovax.common.security.NimbusAuthAdminClient.RawUserInput;
import com.nimbusnovax.common.security.NimbusAuthInternalClient;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Sem checagem de permissão aqui - AdminUserService não tem mais autorização manual
 * (CurrentUserProvider foi removido do construtor), ela migrou pra {@code @CheckSecurity} nos
 * métodos de {@code BffAdminUsersController} (defesa aplicada via proxy AOP do Spring Security,
 * não observável instanciando o service direto com "new" como este teste faz - cobrir a rejeição
 * em si exigiria um teste de integração contra o controller, não existe nenhum ainda pra nenhum
 * controller admin deste projeto). Cobre, principalmente, a regra de "usuário é global no
 * NimbusAuth" - update()/create() precisam preservar grupos de outros apps Nimbus (ex.: Cardsync)
 * que o usuário já tenha, já que PUT /api/v1/users/{id} faz replace total da lista de grupos.
 */
class AdminUserServiceTest {

  private static final String TOKEN = "token";

  private final NimbusAuthAdminClient client = mock(NimbusAuthAdminClient.class);
  private final NimbusAuthInternalClient internalClient = mock(NimbusAuthInternalClient.class);
  private final AdminUserService service = new AdminUserService(client, internalClient);

  @Test
  void listOnlyReturnsUsersWithAtLeastOneNimbusNovaxGroup() {
    RawUser nimbusNovaxUser = rawUser("fiscal@acquamania.com.br",
        List.of(new RawGroupOption(UUID.randomUUID(), "GESTOR", "d", "nimbusnovax")));
    RawUser cardSyncOnlyUser = rawUser("outro@cardsync.com.br",
        List.of(new RawGroupOption(UUID.randomUUID(), "ADMINISTRADOR", "d", "cardsync")));
    when(client.searchAllUsers(TOKEN)).thenReturn(List.of(nimbusNovaxUser, cardSyncOnlyUser));

    List<AdminUserResponse> result = service.list(TOKEN);

    assertThat(result).extracting(AdminUserResponse::userName).containsExactly("fiscal@acquamania.com.br");
  }

  @Test
  void updatePreservesGroupsFromOtherApps() {
    UUID userId = UUID.randomUUID();
    UUID cardSyncGroupId = UUID.randomUUID();
    UUID oldNimbusNovaxGroupId = UUID.randomUUID();
    UUID newNimbusNovaxGroupId = UUID.randomUUID();

    RawUser current = rawUser("user@acquamania.com.br", List.of(
        new RawGroupOption(cardSyncGroupId, "ADMINISTRADOR", "d", "cardsync"),
        new RawGroupOption(oldNimbusNovaxGroupId, "GESTOR", "d", "nimbusnovax")));
    when(client.getUser(TOKEN, userId)).thenReturn(current);
    when(client.updateUser(eq(TOKEN), eq(userId), any())).thenReturn(current);

    AdminUserRequest request = new AdminUserRequest(
        "user@acquamania.com.br", "User", "12345678901", Set.of(newNimbusNovaxGroupId));

    service.update(TOKEN, userId, request);

    var captor = org.mockito.ArgumentCaptor.forClass(RawUserInput.class);
    verify(client).updateUser(eq(TOKEN), eq(userId), captor.capture());

    assertThat(captor.getValue().groupIds())
        .containsExactlyInAnyOrder(cardSyncGroupId, newNimbusNovaxGroupId)
        .doesNotContain(oldNimbusNovaxGroupId);
  }

  @Test
  void createCreatesNewUserWhenUserNameDoesNotExistGlobally() {
    when(client.searchAllUsers(TOKEN)).thenReturn(List.of(rawUser("other@acquamania.com.br", List.of())));
    UUID groupId = UUID.randomUUID();
    AdminUserRequest request = new AdminUserRequest("new@acquamania.com.br", "New", "12345678901", Set.of(groupId));
    when(client.createUser(eq(TOKEN), any())).thenReturn(rawUser("new@acquamania.com.br", List.of()));

    service.create(TOKEN, request);

    verify(client).createUser(eq(TOKEN), any());
    verify(client, never()).updateUser(any(), any(), any());
  }

  @Test
  void createGrantsAccessByMergingGroupsWhenUserAlreadyExistsGlobally() {
    UUID existingId = UUID.randomUUID();
    UUID cardSyncGroupId = UUID.randomUUID();
    UUID newNimbusNovaxGroupId = UUID.randomUUID();

    RawUser existing = new RawUser(existingId, "Já Existe", "Ja.Existe@Cardsync.com.br", "98765432100", 1,
        null, null, null, null, null,
        List.of(new RawGroupOption(cardSyncGroupId, "ADMINISTRADOR", "d", "cardsync")));
    when(client.searchAllUsers(TOKEN)).thenReturn(List.of(existing));
    when(client.updateUser(eq(TOKEN), eq(existingId), any())).thenReturn(existing);

    // e-mail digitado no formulário de "cadastro" bate (case-insensitive) com o usuário já existente.
    AdminUserRequest request = new AdminUserRequest(
        "ja.existe@cardsync.com.br", "Nome Diferente Digitado Agora", "00000000000",
        Set.of(newNimbusNovaxGroupId));

    service.create(TOKEN, request);

    verify(client, never()).createUser(any(), any());
    var captor = org.mockito.ArgumentCaptor.forClass(RawUserInput.class);
    verify(client).updateUser(eq(TOKEN), eq(existingId), captor.capture());

    assertThat(captor.getValue().groupIds()).containsExactlyInAnyOrder(cardSyncGroupId, newNimbusNovaxGroupId);
    // identidade do usuário existente é preservada, não sobrescrita pelo formulário de cadastro.
    assertThat(captor.getValue().userName()).isEqualTo("Ja.Existe@Cardsync.com.br");
    assertThat(captor.getValue().name()).isEqualTo("Já Existe");
    assertThat(captor.getValue().document()).isEqualTo("98765432100");
  }

  @Test
  void optionsDelegatesToInternalClientSortedByName() {
    UUID userId = UUID.randomUUID();
    when(internalClient.fetchOptionsByAppKey("nimbusnovax"))
        .thenReturn(List.of(new NimbusAuthInternalClient.UserSummary(userId, "fiscal@acquamania.com.br", "Fiscal")));

    List<AdminUserMinimalResponse> result = service.options();

    assertThat(result).containsExactly(new AdminUserMinimalResponse(userId, "Fiscal", "fiscal@acquamania.com.br"));
  }

  @Test
  void activateBulkDelegatesToClient() {
    List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());

    service.activateBulk(TOKEN, ids);

    verify(client).activateUsersBulk(TOKEN, ids);
  }

  @Test
  void deactivateBulkDelegatesToClient() {
    List<UUID> ids = List.of(UUID.randomUUID());

    service.deactivateBulk(TOKEN, ids);

    verify(client).deactivateUsersBulk(TOKEN, ids);
  }

  private RawUser rawUser(String userName, List<RawGroupOption> groups) {
    return new RawUser(UUID.randomUUID(), "Nome", userName, "12345678901", 1, null, null, null, null, null, groups);
  }
}
