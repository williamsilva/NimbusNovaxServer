package com.nimbusnovax.common.company;

import com.nimbussystems.commons.notification.mail.EmailProperties;
import com.nimbussystems.commons.security.CurrentUserProvider;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Dados da empresa emissora do voucher (nome/CNPJ/endereço/telefone/logo) - linha única, mesmo
 *  padrão de {@code EmailSettingsService}. Consumido pelo cabeçalho do e-mail/PDF de voucher (ver
 *  VoucherFlowService.buildDocumentContext) via {@link #getOrDefaultModel()}, sem checagem de
 *  permissão (uso interno, não é uma ação disparada pelo usuário). */
@Service
@RequiredArgsConstructor
@Transactional
public class CompanySettingsService {

  /** Caminho servido sem autenticação por {@code PublicCompanyLogoController} - fora de
   *  /api/** e /bff/** de propósito (ver SecurityConfig: uma request que não casa com nenhuma das
   *  duas security filter chains passa direto, sem exigir sessão/JWT), porque quem busca essa URL
   *  é o cliente de e-mail do destinatário do voucher (e o próprio openhtmltopdf, na geração do
   *  PDF) - nenhum dos dois tem cookie de sessão nem Bearer token. */
  static final String LOGO_PATH = "/public/company-logo";

  private static final Set<String> ALLOWED_LOGO_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
  private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024 * 1024;

  private final CompanySettingsRepository repository;
  private final CurrentUserProvider currentUserProvider;
  private final EmailProperties emailProperties;

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

  public CompanySettingsModel updateLogo(MultipartFile file) {
    requireAuthority("COMPANY_SETTINGS_PROCESS");
    validateLogo(file);

    CompanySettingsEntity settings = findOrNew();
    try {
      settings.setLogoData(file.getBytes());
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falha ao ler o arquivo da logo", e);
    }
    settings.setLogoContentType(file.getContentType());
    settings.setUpdatedById(currentUserId());
    return toModel(repository.save(settings));
  }

  public CompanySettingsModel deleteLogo() {
    requireAuthority("COMPANY_SETTINGS_PROCESS");

    CompanySettingsEntity settings = findOrNew();
    settings.setLogoData(null);
    settings.setLogoContentType(null);
    settings.setUpdatedById(currentUserId());
    return toModel(repository.save(settings));
  }

  /** Uso interno de {@code PublicCompanyLogoController} - sem checagem de permissão (mesmo motivo
   *  de {@link #getOrDefaultModel()}: quem busca essa URL é o cliente de e-mail do destinatário do
   *  voucher, não um usuário autenticado do sistema). Vazio (não erro) quando nenhuma logo foi
   *  enviada ainda, pra o controller devolver 404 em vez de vazar stacktrace. */
  @Transactional(readOnly = true)
  public Optional<LogoData> findLogo() {
    return repository.findFirstBy()
        .filter(settings -> settings.getLogoData() != null)
        .map(settings -> new LogoData(settings.getLogoData(), settings.getLogoContentType()));
  }

  public record LogoData(byte[] data, String contentType) {
  }

  private void validateLogo(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo de logo vazio.");
    }
    if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A logo deve ter no máximo 2MB.");
    }
    if (!ALLOWED_LOGO_CONTENT_TYPES.contains(file.getContentType())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato inválido - use PNG, JPEG ou WEBP.");
    }
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
        settings.getEmail(),
        settings.getLogoData() != null ? emailProperties.getPublicBaseUrl() + LOGO_PATH : null);
  }
}
