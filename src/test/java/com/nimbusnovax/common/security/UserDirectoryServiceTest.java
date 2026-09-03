package com.nimbusnovax.common.security;

import com.nimbussystems.commons.security.UserMinimalResponse;

import com.nimbussystems.commons.security.UserDirectoryRepository;

import com.nimbussystems.commons.security.UserDirectoryEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusnovax.common.security.NimbusAuthInternalClient.UserSummary;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserDirectoryServiceTest {

  private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

  private final UserDirectoryRepository repository = mock(UserDirectoryRepository.class);
  private final NimbusAuthInternalClient client = mock(NimbusAuthInternalClient.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final UserDirectoryService service = new UserDirectoryService(repository, client, clock);

  @Test
  void summaryForReturnsNullWithoutCallingRemoteWhenUserIdIsNull() {
    Optional<UserMinimalResponse> result = service.summaryFor(null);

    assertThat(result).isEmpty();
    verify(client, never()).fetchUsers(any());
  }

  /** Janela curta (STALE_AFTER = 1 min) - dentro dela, o registro local já basta, sem bater no
   *  NimbusAuth de novo (evita 1 chamada HTTP por item repetido na mesma renderização de lista). */
  @Test
  void summaryForReusesLocalCopyWithoutCallingRemoteWhenWithinStaleWindow() {
    UUID userId = UUID.randomUUID();
    UserDirectoryEntity local = entityOf(userId, "Nome Atual", "atual", NOW.minusSeconds(30));
    when(repository.findById(userId)).thenReturn(Optional.of(local));

    Optional<UserMinimalResponse> result = service.summaryFor(userId);

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("Nome Atual");
    verify(client, never()).fetchUsers(any());
  }

  /** Fora da janela, resolve ao vivo de novo e atualiza o registro local - é isso que faz uma
   *  troca de nome no NimbusAuth aparecer sem precisar de restart/deploy (ver STALE_AFTER). */
  @Test
  void summaryForCallsRemoteAndUpdatesLocalCopyWhenStale() {
    UUID userId = UUID.randomUUID();
    UserDirectoryEntity local = entityOf(userId, "Nome Antigo", "user", NOW.minusSeconds(120));
    when(repository.findById(userId)).thenReturn(Optional.of(local));
    when(client.fetchUsers(List.of(userId)))
        .thenReturn(List.of(new UserSummary(userId, "user", "Nome Novo")));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Optional<UserMinimalResponse> result = service.summaryFor(userId);

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("Nome Novo");
  }

  /** NimbusAuthInternalClient.fetchUsers já degrada pra lista vazia em qualquer falha (timeout,
   *  NimbusAuth fora do ar) - nesse caso o registro local (mesmo desatualizado) segue servindo de
   *  fallback, pra resolver nome de usuário nunca derrubar a listagem por causa disso. */
  @Test
  void summaryForFallsBackToLocalCopyWhenRemoteCallFails() {
    UUID userId = UUID.randomUUID();
    UserDirectoryEntity local = entityOf(userId, "Nome Antigo", "user", NOW.minusSeconds(120));
    when(repository.findById(userId)).thenReturn(Optional.of(local));
    when(client.fetchUsers(List.of(userId))).thenReturn(List.of());

    Optional<UserMinimalResponse> result = service.summaryFor(userId);

    assertThat(result).isPresent();
    assertThat(result.get().name()).isEqualTo("Nome Antigo");
  }

  @Test
  void summaryForReturnsEmptyWhenNoLocalCopyAndRemoteCallFails() {
    UUID userId = UUID.randomUUID();
    when(repository.findById(userId)).thenReturn(Optional.empty());
    when(client.fetchUsers(List.of(userId))).thenReturn(List.of());

    Optional<UserMinimalResponse> result = service.summaryFor(userId);

    assertThat(result).isEmpty();
  }

  private static UserDirectoryEntity entityOf(UUID id, String name, String username, Instant syncedAt) {
    UserDirectoryEntity entity = new UserDirectoryEntity();
    entity.setId(id);
    entity.setName(name);
    entity.setUsername(username);
    entity.setSyncedAt(OffsetDateTime.ofInstant(syncedAt, ZoneOffset.UTC));
    return entity;
  }
}
