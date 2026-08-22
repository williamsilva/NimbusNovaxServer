package com.nimbusnovax.common.company;

import com.nimbusnovax.common.security.CurrentUserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Dados da empresa emissora do voucher (nome/CNPJ/endereço/telefone) - linha única, mesmo padrão
 *  de {@code EmailSettingsService}. Consumido pelo cabeçalho do e-mail/PDF de voucher (ver
 *  VoucherFlowService.buildDocumentContext) via {@link #getOrDefaultModel()}, sem checagem de
 *  permissão (uso interno, não é uma ação disparada pelo usuário). */
@Service
@RequiredArgsConstructor
@Transactional
public class CompanySettingsService {

  private final CompanySettingsRepository repository;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public CompanySettingsModel getSettings() {
    requireAuthority("COMPANY_SETTINGS_CONSULT");
    return toModel(findOrNew());
  }

  public CompanySettingsModel update(CompanySettingsRequest request) {
    requireAuthority("COMPANY_SETTINGS_PROCESS");
    CompanySettingsEntity settings = findOrNew();
    settings.setName(request.name());
    settings.setDocument(request.document());
    settings.setAddressLine(request.addressLine());
    settings.setCity(request.city());
    settings.setState(request.state());
    settings.setPostalCode(request.postalCode());
    settings.setPhone(request.phone());
    settings.setEmail(request.email());
    settings.setUpdatedById(currentUserId());
    return toModel(repository.save(settings));
  }

  /** Uso interno (montagem do e-mail/PDF de voucher, ver VoucherFlowService.buildDocumentContext)
   *  - sem checagem de permissão, quem envia um voucher não precisa poder editar a empresa. Nunca
   *  quebra se a empresa ainda não foi configurada - devolve um model em branco (cabeçalho vazio
   *  em vez de impedir o envio do voucher). */
  @Transactional(readOnly = true)
  public CompanySettingsModel getOrDefaultModel() {
    return toModel(findOrNew());
  }

  private CompanySettingsEntity findOrNew() {
    return repository.findFirstBy().orElseGet(CompanySettingsEntity::new);
  }

  private UUID currentUserId() {
    try {
      return UUID.fromString(currentUserProvider.requireUserId());
    } catch (IllegalStateException | IllegalArgumentException e) {
      return null;
    }
  }

  private void requireAuthority(String permission) {
    if (!currentUserProvider.hasAuthority("PERM_" + permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing " + permission + " authority");
    }
  }

  private CompanySettingsModel toModel(CompanySettingsEntity settings) {
    return new CompanySettingsModel(
        settings.getName(),
        settings.getDocument(),
        settings.getAddressLine(),
        settings.getCity(),
        settings.getState(),
        settings.getPostalCode(),
        settings.getPhone(),
        settings.getEmail());
  }
}
