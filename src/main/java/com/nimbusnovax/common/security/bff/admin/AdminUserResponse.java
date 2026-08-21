package com.nimbusnovax.common.security.bff.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Espelha UserModel do NimbusAuth (GET/POST/PUT /api/v1/users/**), mas "groups" aqui já vem
 * filtrado só pelos grupos do NimbusNovax (ver AdminUserService) - ao contrário de ProfileResponse
 * (self-service), esta tela é administrativa e pode legitimamente mostrar/editar grupos, mas só
 * os do próprio app; grupos de outros apps Nimbus (ex.: Cardsync) que o usuário também tenha
 * nunca aparecem nem são afetados (ver merge em AdminUserService.update).
 */
public record AdminUserResponse(
    UUID id,
    String name,
    String userName,
    String document,
    Integer status,
    Instant createdAt,
    Instant lastLoginAt,
    Instant blockedUntil,
    Instant passwordExpiresAt,
    AdminUserMinimalResponse createdBy,
    List<AdminGroupOptionResponse> groups) {
}
