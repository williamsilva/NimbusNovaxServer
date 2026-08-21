package com.nimbusnovax.common.security.bff.admin;

import java.time.Instant;
import java.util.UUID;

/**
 * Versão da listagem de grupos (GET /bff/v1/groups) - ao contrário de AdminGroupOptionResponse
 * (usado dentro de AdminUserResponse.groups, só id/name/description), traz contadores/data/autor
 * pra bater com a tabela de "Grupos" (colunas Cadastrado em/por, Usuários, Permissões). Sem
 * "permissions" (lista completa) - isso só vem no detalhe (AdminGroupResponse, GET /{id}).
 */
public record AdminGroupSummaryResponse(
    UUID id,
    String name,
    String description,
    Integer usersCount,
    Integer permissionsCount,
    Instant createdAt,
    AdminUserMinimalResponse createdBy) {
}
