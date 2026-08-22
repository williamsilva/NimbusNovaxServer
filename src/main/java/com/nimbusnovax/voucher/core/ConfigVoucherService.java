package com.nimbusnovax.voucher.core;

import com.nimbusnovax.common.security.CurrentUserProvider;
import com.nimbusnovax.voucher.dto.request.ConfigVoucherRequest;
import com.nimbusnovax.voucher.dto.response.ConfigVoucherResponse;
import com.nimbusnovax.voucher.model.ConfigVoucher;
import com.nimbusnovax.voucher.repository.ConfigVoucherRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Configuração de voucher (dias para expirar/cancelar, número de vouchers pendentes permitido,
 *  notificação por e-mail) - linha única identificada pela chave {@link #KEY}, mesmo papel de
 *  {@code ws_config_voucher}/{@code ConfigVoucherService} no sistema legado (Novax antigo). */
@Service
@RequiredArgsConstructor
@Transactional
public class ConfigVoucherService {

  public static final String KEY = "VOUCHER_CHANGE";

  private final ConfigVoucherRepository repository;
  private final CurrentUserProvider currentUserProvider;

  @Transactional(readOnly = true)
  public ConfigVoucherResponse find() {
    requireAuthority("VOUCHER_CONFIG_CONSULT");
    return toResponse(getOrCreate());
  }

  public ConfigVoucherResponse update(ConfigVoucherRequest request) {
    requireAuthority("VOUCHER_CONFIG_CHANGE");
    ConfigVoucher config = getOrCreate();
    config.setSenderMail(request.senderMail() == null ? Boolean.TRUE : request.senderMail());
    config.setDaysToExpire(request.daysToExpire());
    config.setDaysToCancel(request.daysToCancel());
    config.setNumberPendingVouchers(request.numberPendingVouchers());
    config.setEmailBody(request.emailBody());
    config.setUpdatedById(currentUserId());
    return toResponse(repository.save(config));
  }

  /** Uso interno de VoucherService/VoucherFlowService/VoucherScheduledTasks - sem checagem de
   *  permissão, não é uma ação disparada diretamente pelo usuário. */
  public ConfigVoucher getOrCreate() {
    return repository.findByKey(KEY).orElseGet(this::createDefault);
  }

  private ConfigVoucher createDefault() {
    ConfigVoucher config = new ConfigVoucher();
    config.setKey(KEY);
    config.setSenderMail(Boolean.TRUE);
    config.setDaysToExpire(20);
    config.setDaysToCancel(90);
    config.setNumberPendingVouchers(20);
    return repository.save(config);
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

  private ConfigVoucherResponse toResponse(ConfigVoucher config) {
    return new ConfigVoucherResponse(
        config.getId(),
        config.getSenderMail(),
        config.getDaysToExpire(),
        config.getDaysToCancel(),
        config.getNumberPendingVouchers(),
        config.getEmailBody(),
        config.getCreatedAt(),
        config.getUpdatedAt());
  }
}
