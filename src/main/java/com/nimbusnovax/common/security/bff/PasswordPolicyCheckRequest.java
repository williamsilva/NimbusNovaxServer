package com.nimbusnovax.common.security.bff;

/**
 * Sem "username" de propósito, diferente do PasswordCheckRequest do NimbusAuth - o BFF já sabe
 * quem é o usuário autenticado (CurrentUserProvider) e injeta antes de encaminhar; não faz
 * sentido confiar num username vindo do cliente pra essa checagem (regra "não conter usuário").
 */
public record PasswordPolicyCheckRequest(String password, String confirmPassword) {
}
