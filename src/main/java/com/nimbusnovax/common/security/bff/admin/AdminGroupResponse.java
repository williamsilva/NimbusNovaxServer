package com.nimbusnovax.common.security.bff.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Espelha GroupModel do NimbusAuth (GET /api/v1/groups/{id}), incluindo "users" - a tela de
 * Grupos também administra membros por aqui (aba "Usuários" do detalhe do grupo), além do fluxo
 * já existente pela tela de Usuários via groupIds (AdminUserRequest); os dois caminhos convergem
 * pro mesmo PUT /api/v1/groups/{id}/users no NimbusAuth.
 */
public record AdminGroupResponse(
    UUID id,
    String name,
    String description,
    Integer usersCount,
    Integer permissionsCount,
    Instant createdAt,
    List<AdminPermissionOptionResponse> permissions,
    List<AdminUserMinimalResponse> users) {
}
