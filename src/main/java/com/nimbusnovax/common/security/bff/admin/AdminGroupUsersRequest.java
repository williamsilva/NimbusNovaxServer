package com.nimbusnovax.common.security.bff.admin;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/** Mapeia pro GroupUsersInput do NimbusAuth (PUT /api/v1/groups/{id}/users) - a lista é um
 *  replace total, não incremental (ver GroupService.updateUsers no NimbusAuth). */
public record AdminGroupUsersRequest(@NotNull List<UUID> userIds) {
}
