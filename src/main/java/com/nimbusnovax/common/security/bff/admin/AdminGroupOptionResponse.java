package com.nimbusnovax.common.security.bff.admin;

import java.util.UUID;

/** Espelha GroupOptionModel do NimbusAuth, sem o campo appKey - aqui é sempre "nimbusnovax" (a
 *  listagem já é filtrada por appKey em NimbusAuthAdminClient), não precisa ir pro frontend. */
public record AdminGroupOptionResponse(UUID id, String name, String description) {
}
