package com.nimbusnovax.administracao.dto.request;

import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
    @NotBlank String name,
    String description,
    @NotNull TypeProductEnum typeProduct,
    @NotNull BigDecimal amount,
    LocalDate initialValidate,
    LocalDate finalValidate,
    StatusEnum status) {
}
