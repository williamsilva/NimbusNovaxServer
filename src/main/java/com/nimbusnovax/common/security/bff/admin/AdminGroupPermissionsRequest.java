package com.nimbusnovax.common.security.bff.admin;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Mapeia pro GroupPermissionsInput do NimbusAuth (PUT /api/v1/groups/{id}/permissions) - a lista
 *  é um replace total, não incremental (ver GroupService.updatePermissions no NimbusAuth). */
public record AdminGroupPermissionsRequest(@NotNull List<UUID> permissionIds) {
}
