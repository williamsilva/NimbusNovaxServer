package com.nimbusnovax.common.security.bff.admin;

import java.util.UUID;

/**
 * Espelha UserMinimalModel do NimbusAuth (id/name/userName) - usado tanto como "criado por" em
 * usuários/grupos quanto como item dos seletores de usuário (GET /bff/v1/users/options[-filter]).
 */
public record AdminUserMinimalResponse(UUID id, String name, String userName) {
}
