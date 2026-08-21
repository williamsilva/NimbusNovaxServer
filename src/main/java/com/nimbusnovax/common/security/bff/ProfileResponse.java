package com.nimbusnovax.common.security.bff;

import java.time.Instant;
import java.util.UUID;

/**
 * Espelha o UserModel do NimbusAuth (GET /api/v1/me/profile), mas sem o campo "groups" dele - ali
 * não é filtrado por app_key (achado documentado na Fase de auditoria/i18n), então exporia grupos
 * de outros apps (ex.: Cardsync) pra quem tiver conta em mais de um. Grupos/permissões do
 * NimbusNovax em si já vêm corretamente filtrados via /bff/me (claims do token, não este endpoint).
 */
public record ProfileResponse(
    UUID id,
    String name,
    String userName,
    String document,
    Integer status,
    Instant createdAt,
    Instant lastLoginAt,
    Instant blockedUntil,
    Instant passwordExpiresAt) {
}
