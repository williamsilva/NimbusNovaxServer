package com.nimbusnovax.common.company;

public record CompanySettingsModel(
    String name,
    String document,
    String addressLine,
    String city,
    String state,
    String postalCode,
    String phone,
    String email,
    /** URL pública (sem autenticação, ver PublicCompanyLogoController) da logo configurada, ou
     *  null se nenhuma logo foi enviada ainda - usada tanto na prévia da tela Configurações &gt;
     *  Empresa quanto no cabeçalho do e-mail/PDF do voucher (ver voucher-pdf.html/
     *  send-voucher.html/change-voucher.html/warning-voucher-expired.html). */
    String logoUrl) {
}
