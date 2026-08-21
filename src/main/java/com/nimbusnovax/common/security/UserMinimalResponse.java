package com.nimbusnovax.common.security;

import java.util.UUID;

/**
 * Resumo de usuário (id/nome/username) resolvido via UserDirectoryService - usado só pra exibir
 * "solicitado por"/"aprovado por"/etc. em respostas de outros domínios (ex.: Aditivo). Mesmo
 * shape do UserMinimalModel já usado no frontend pra Grupos (id/name/userName).
 */
public record UserMinimalResponse(UUID id, String name, String userName) {
}
