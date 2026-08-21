package com.nimbusnovax.administracao.dto.response;

import com.nimbusnovax.administracao.model.enums.GenerationEnum;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import java.time.Instant;
import java.util.UUID;

public record CancellationReasonResponse(
    UUID id,
    String name,
    String description,
    StatusEnum status,
    GenerationEnum generation,
    Instant createdAt,
    Instant updatedAt) {
}
