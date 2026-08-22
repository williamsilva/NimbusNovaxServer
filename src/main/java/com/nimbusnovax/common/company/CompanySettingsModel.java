package com.nimbusnovax.common.company;

public record CompanySettingsModel(
    String name,
    String document,
    String addressLine,
    String city,
    String state,
    String postalCode,
    String phone,
    String email) {
}
