package com.nimbusnovax.common.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanySettingsRequest(
    @NotBlank @Size(max = 255) String name,
    @Size(max = 20) String document,
    @Size(max = 255) String addressLine,
    @Size(max = 100) String city,
    @Size(max = 2) String state,
    @Size(max = 20) String postalCode,
    @Size(max = 20) String phone,
    @Size(max = 255) String email) {
}
