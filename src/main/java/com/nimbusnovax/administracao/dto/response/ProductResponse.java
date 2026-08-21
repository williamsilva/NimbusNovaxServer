package com.nimbusnovax.administracao.dto.response;

import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String name,
    String description,
    TypeProductEnum typeProduct,
    BigDecimal amount,
    LocalDate initialValidate,
    LocalDate finalValidate,
    StatusEnum status,
    Instant createdAt,
    Instant updatedAt) {
}
