package com.nimbusnovax.common.security.bff.admin;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

/** Mapeia pro ListIdsInput do NimbusAuth (POST /api/v1/users/activate|deactivate em lote). */
public record AdminBulkIdsRequest(@NotEmpty List<UUID> ids) {
}
