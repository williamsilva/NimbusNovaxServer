-- Logo da empresa (opcional) - exibida no cabeçalho do e-mail e do PDF do voucher enviado ao
-- cliente (ver CompanySettingsEntity/Service, BffCompanySettingsController, PublicCompanyLogoController).
-- Guardada como bytes direto no banco (não em bucket S3, diferente de MeasurementMedia/
-- TicketClosePhoto no NimbusFlow) - imagem única e pequena (linha única de company_settings,
-- limite de 2MB aplicado em CompanySettingsService), não vale a complexidade extra de storage
-- externo pra um caso desse tamanho.

ALTER TABLE company_settings ADD COLUMN logo_data BYTEA;
ALTER TABLE company_settings ADD COLUMN logo_content_type VARCHAR(100);
