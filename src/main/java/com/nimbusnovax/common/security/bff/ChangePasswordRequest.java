package com.nimbusnovax.common.security.bff;

import jakarta.validation.constraints.NotBlank;

/** Espelha o ChangeMyPasswordModel do NimbusAuth (PUT /api/v1/me/password/change). */
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword,
    @NotBlank String confirmPassword) {
}
