package com.nimbusnovax.administracao.dto.request;

import com.nimbusnovax.administracao.model.enums.StatusEnum;
import jakarta.validation.constraints.NotBlank;

/** {@code generation} não faz parte do request - o service sempre força USER no save (ver
 *  CancellationReasonService), mesma regra do sistema legado. */
public record CancellationReasonRequest(@NotBlank String name, String description, StatusEnum status) {
}
