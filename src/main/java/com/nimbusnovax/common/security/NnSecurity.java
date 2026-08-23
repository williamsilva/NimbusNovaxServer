package com.nimbusnovax.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bean resolvido pelas expressões SpEL de {@link CheckSecurity} ({@code @nnSecurity.canXyz()}) —
 * cada método aqui é só um alias nomeado para {@link CurrentUserProvider#hasAuthority(String)},
 * mesma checagem (mesmo bypass de {@code ROLE_SUPPORT}) que já era feita imperativamente dentro
 * de cada Service via {@code requireAuthority(String)}. Nome do bean é "nnSecurity" (default do
 * Spring a partir do nome da classe) — não renomear sem atualizar as expressões em
 * {@link CheckSecurity}.
 */
@Component
@RequiredArgsConstructor
public class NnSecurity {

  private final CurrentUserProvider currentUserProvider;

  /* Voucher */
  public boolean canConsultVouchers() {
    return currentUserProvider.hasAuthority("PERM_VOUCHERS_CONSULT");
  }

  public boolean canCreateVouchers() {
    return currentUserProvider.hasAuthority("PERM_VOUCHERS_CREATE");
  }

  public boolean canChangeVouchers() {
    return currentUserProvider.hasAuthority("PERM_VOUCHERS_CHANGE");
  }

  public boolean canDeleteVouchers() {
    return currentUserProvider.hasAuthority("PERM_VOUCHERS_DELETE");
  }

  /* Product */
  public boolean canConsultProducts() {
    return currentUserProvider.hasAuthority("PERM_PRODUTOS_CONSULT");
  }

  public boolean canCreateProducts() {
    return currentUserProvider.hasAuthority("PERM_PRODUTOS_CREATE");
  }

  public boolean canChangeProducts() {
    return currentUserProvider.hasAuthority("PERM_PRODUTOS_CHANGE");
  }

  public boolean canDeleteProducts() {
    return currentUserProvider.hasAuthority("PERM_PRODUTOS_DELETE");
  }

  /* Cancellation Reason */
  public boolean canConsultCancellationReasons() {
    return currentUserProvider.hasAuthority("PERM_MOTIVO_CANCELAMENTO_CONSULT");
  }

  public boolean canCreateCancellationReasons() {
    return currentUserProvider.hasAuthority("PERM_MOTIVO_CANCELAMENTO_CREATE");
  }

  public boolean canChangeCancellationReasons() {
    return currentUserProvider.hasAuthority("PERM_MOTIVO_CANCELAMENTO_CHANGE");
  }

  public boolean canDeleteCancellationReasons() {
    return currentUserProvider.hasAuthority("PERM_MOTIVO_CANCELAMENTO_DELETE");
  }

  /* Email Log */
  public boolean canConsultEmailLog() {
    return currentUserProvider.hasAuthority("PERM_EMAIL_LOG_CONSULT");
  }

  /* Agent */
  public boolean canConsultAgents() {
    return currentUserProvider.hasAuthority("PERM_AGENTES_CONSULT");
  }

  public boolean canCreateAgents() {
    return currentUserProvider.hasAuthority("PERM_AGENTES_CREATE");
  }

  public boolean canChangeAgents() {
    return currentUserProvider.hasAuthority("PERM_AGENTES_CHANGE");
  }

  public boolean canDeleteAgents() {
    return currentUserProvider.hasAuthority("PERM_AGENTES_DELETE");
  }

  /* User (segurança > usuários) */
  public boolean canConsultUsers() {
    return currentUserProvider.hasAuthority("PERM_USERS_CONSULT");
  }

  public boolean canCreateUsers() {
    return currentUserProvider.hasAuthority("PERM_USERS_CREATE");
  }

  public boolean canChangeUsers() {
    return currentUserProvider.hasAuthority("PERM_USERS_CHANGE");
  }

  public boolean canActiveOrInactiveUsers() {
    return currentUserProvider.hasAuthority("PERM_USERS_ACTIVE_OR_INACTIVE");
  }

  public boolean canResendInviteUsers() {
    return currentUserProvider.hasAuthority("PERM_USERS_RESEND_INVITE");
  }

  /* Group (segurança > grupos) */
  public boolean canConsultGroups() {
    return currentUserProvider.hasAuthority("PERM_GROUPS_CONSULT");
  }

  public boolean canCreateGroups() {
    return currentUserProvider.hasAuthority("PERM_GROUPS_CREATE");
  }

  public boolean canChangeGroups() {
    return currentUserProvider.hasAuthority("PERM_GROUPS_CHANGE");
  }

  public boolean canDeleteGroups() {
    return currentUserProvider.hasAuthority("PERM_GROUPS_DELETE");
  }

  public boolean canManagePermissionGroups() {
    return currentUserProvider.hasAuthority("PERM_GROUPS_MANAGEMENT_PERMISSION");
  }

  public boolean canManageUserGroups() {
    return currentUserProvider.hasAuthority("PERM_GROUPS_MANAGEMENT_USER");
  }

  /* Voucher Config */
  public boolean canConsultVoucherConfig() {
    return currentUserProvider.hasAuthority("PERM_VOUCHER_CONFIG_CONSULT");
  }

  public boolean canChangeVoucherConfig() {
    return currentUserProvider.hasAuthority("PERM_VOUCHER_CONFIG_CHANGE");
  }
}
