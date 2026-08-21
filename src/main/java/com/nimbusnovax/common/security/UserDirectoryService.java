package com.nimbusnovax.common.security;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolve nome/username de um usuário (id emitido pelo NimbusAuth) para exibição em campos de
 * auditoria (ex.: requestedBy/approvedBy de Aditivo) das respostas da API do NimbusNovax. Mesmo
 * papel/estratégia do UserDirectoryService do CardsyncServer: cache local (user_directory) +
 * busca sob demanda no NimbusAuth quando o id não está (ou está velho) no cache local, sem
 * fila/evento - "lazy fetch + upsert".
 *
 * <p>SEM {@code @Cacheable} de propósito (removido em 2026-08-18) - havia um cache em memória
 * (ConcurrentMapCacheManager default do Spring Boot, sem TTL/eviction) por cima da checagem de
 * staleness abaixo; como nunca expirava dentro da vida do processo, a 1ª resolução de cada
 * usuário ficava congelada pro resto do uptime do container - uma troca de nome no NimbusAuth só
 * refletia depois do próximo restart/deploy, o oposto do "resolver em tempo real" pedido pro
 * Ticket/ActionPlan/Addendum (ver TicketService/ActionPlanService/AddendumApprovalService). A
 * tabela {@code user_directory} (STALE_AFTER abaixo) já é o único cache que resta.
 */
@Service
@RequiredArgsConstructor
public class UserDirectoryService {

  // Curto de propósito (era 24h) - só pra evitar 1 chamada HTTP por item repetido dentro da MESMA
  // renderização de lista (ex.: 20 chamados do mesmo solicitante numa página), não pra servir de
  // cache de longo prazo - 24h fazia uma troca de nome no NimbusAuth demorar até 24h pra aparecer.
  private static final Duration STALE_AFTER = Duration.ofMinutes(1);

  private final UserDirectoryRepository repository;
  private final NimbusAuthInternalClient client;
  private final Clock clock;

  @Transactional
  public Optional<UserMinimalResponse> summaryFor(UUID userId) {
    if (userId == null) {
      return Optional.empty();
    }

    UserDirectoryEntity local = repository.findById(userId).orElse(null);
    OffsetDateTime now = OffsetDateTime.now(clock);

    if (local == null || local.getSyncedAt().isBefore(now.minus(STALE_AFTER))) {
      var remote = client.fetchUsers(List.of(userId)).stream().findFirst().orElse(null);

      if (remote != null) {
        UserDirectoryEntity toSave = local != null ? local : new UserDirectoryEntity();
        toSave.setId(userId);
        toSave.setUsername(remote.username());
        toSave.setName(remote.name());
        toSave.setSyncedAt(now);
        local = repository.save(toSave);
      }
    }

    if (local == null) {
      return Optional.empty();
    }

    return Optional.of(new UserMinimalResponse(local.getId(), local.getName(), local.getUsername()));
  }

  /**
   * Parse defensivo pra quem guarda o id como String cru (ex.: Addendum.approvedById, que não é
   * UUID nativo na entidade) - id malformado/nulo devolve null em vez de estourar. Só faz o
   * parse, não chama summaryFor(UUID) - fica a cargo do chamador (ex.: AddendumApprovalService)
   * usar o bean injetado pra isso.
   */
  public static UUID parseIdOrNull(String userId) {
    if (userId == null || userId.isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(userId);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
