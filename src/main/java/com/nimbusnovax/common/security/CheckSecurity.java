package com.nimbusnovax.common.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Checagem de permissão declarativa nos endpoints, mesmo padrão do CardSync ({@code
 * com.cardsync.core.security.CheckSecurity}) — cada anotação delega, via SpEL, para um método de
 * {@link NnSecurity} nomeado pela ação ({@code @nnSecurity.canConsultVouchers()}), que por sua vez
 * reaproveita {@link CurrentUserProvider#hasAuthority(String)} (mesma checagem que já existia
 * imperativamente dentro dos Services, incluindo o bypass de {@code ROLE_SUPPORT}) — não duplica
 * lógica de autorização nova, só move onde ela é aplicada: do corpo do Service (imperativo) para a
 * assinatura do Controller (declarativo). Requer {@code @EnableMethodSecurity} em
 * {@link SecurityConfig} para ter efeito.
 */
public @interface CheckSecurity {

  @Target(METHOD)
  @Retention(RUNTIME)
  @PreAuthorize("isAuthenticated()")
  @interface Authenticated {
  }

  @interface Voucher {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultVouchers()")
    @interface CanConsult {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canCreateVouchers()")
    @interface CanCreate {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canChangeVouchers()")
    @interface CanChange {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canDeleteVouchers()")
    @interface CanDelete {
    }
  }

  @interface Product {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultProducts()")
    @interface CanConsult {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canCreateProducts()")
    @interface CanCreate {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canChangeProducts()")
    @interface CanChange {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canDeleteProducts()")
    @interface CanDelete {
    }
  }

  @interface CancellationReason {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultCancellationReasons()")
    @interface CanConsult {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canCreateCancellationReasons()")
    @interface CanCreate {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canChangeCancellationReasons()")
    @interface CanChange {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canDeleteCancellationReasons()")
    @interface CanDelete {
    }
  }

  @interface EmailLog {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultEmailLog()")
    @interface CanConsult {
    }
  }

  @interface Agent {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultAgents()")
    @interface CanConsult {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canCreateAgents()")
    @interface CanCreate {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canChangeAgents()")
    @interface CanChange {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canDeleteAgents()")
    @interface CanDelete {
    }
  }

  @interface User {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultUsers()")
    @interface CanConsult {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canCreateUsers()")
    @interface CanCreate {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canChangeUsers()")
    @interface CanChange {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canActiveOrInactiveUsers()")
    @interface CanActiveOrInactive {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canResendInviteUsers()")
    @interface CanResendInvite {
    }
  }

  @interface Group {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultGroups()")
    @interface CanConsult {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canCreateGroups()")
    @interface CanCreate {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canChangeGroups()")
    @interface CanChange {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canDeleteGroups()")
    @interface CanDelete {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canManagePermissionGroups()")
    @interface CanManagePermission {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canManageUserGroups()")
    @interface CanManageUser {
    }
  }

  @interface VoucherConfig {
    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canConsultVoucherConfig()")
    @interface CanConsult {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    @PreAuthorize("@nnSecurity.canChangeVoucherConfig()")
    @interface CanChange {
    }
  }
}
