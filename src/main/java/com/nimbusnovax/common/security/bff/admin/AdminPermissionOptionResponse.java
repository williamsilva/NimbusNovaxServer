package com.nimbusnovax.common.security.bff.admin;

import java.util.UUID;

/** Espelha PermissionOptionModel do NimbusAuth - já vem filtrado por appKey=nimbusnovax (ver
 *  NimbusAuthAdminClient), sem esse campo aqui. */
public record AdminPermissionOptionResponse(UUID id, String name, String description) {
}
