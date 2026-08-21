package com.nimbusnovax.common.security.bff;

import java.util.List;

/** Espelha o PasswordRulesViewModel do NimbusAuth (GET /api/password/policy, POST .../check). */
public record PasswordPolicyResponse(boolean ok, int minLen, int historySize, List<PasswordRuleResponse> rules) {

  /** state: OK | FAIL | PENDING (PENDING = ainda não avaliado, ex.: campo vazio). */
  public record PasswordRuleResponse(String code, String label, String state) {
  }
}
