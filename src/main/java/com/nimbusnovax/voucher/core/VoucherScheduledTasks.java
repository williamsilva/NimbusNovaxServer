package com.nimbusnovax.voucher.core;

import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.repository.CancellationReasonRepository;
import com.nimbusnovax.common.company.CompanySettingsService;
import com.nimbusnovax.common.notification.mail.EmailSenderService;
import com.nimbusnovax.common.security.NimbusAuthInternalClient;
import com.nimbusnovax.voucher.model.ConfigVoucher;
import com.nimbusnovax.voucher.model.Voucher;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import com.nimbusnovax.voucher.repository.VoucherRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jobs agendados replicados do {@code VoucherScheduled} do sistema legado (Novax antigo), mesmos
 * três horários: aviso de vencidos (6h), marcar OVERDUE (4h) e marcar CALLED_OFF automaticamente
 * (4h10). O motivo de cancelamento automático é o registro "System" (generation SYSTEM, protegido
 * contra edição/exclusão na UI - ver CancellationReasonService) migrado do legado ({@code
 * WsConst.SYSTEM}). Destinatários do aviso de vencidos são os usuários com a permissão
 * VOUCHER_NOTIFICATION no NimbusAuth (ver NimbusAuthInternalClient.fetchOptionsByPermission) - não
 * mais uma lista de e-mails configurada manualmente (ConfigVoucher.notificationEmails, removido).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherScheduledTasks {

  private static final String TIME_ZONE = "America/Sao_Paulo";
  private static final String SYSTEM_REASON_NAME = "System";
  private static final String NIMBUSNOVAX_APP_KEY = "nimbusnovax";
  private static final String VOUCHER_NOTIFICATION_PERMISSION = "VOUCHER_NOTIFICATION";

  private final VoucherRepository voucherRepository;
  private final CancellationReasonRepository cancellationReasonRepository;
  private final ConfigVoucherService configVoucherService;
  private final CompanySettingsService companySettingsService;
  private final EmailSenderService emailSenderService;
  private final NimbusAuthInternalClient nimbusAuthInternalClient;

  @Scheduled(cron = "0 0 6 * * *", zone = TIME_ZONE)
  @Transactional(readOnly = true)
  public void warnExpiredVouchers() {
    List<Voucher> vouchers = voucherRepository.findByStatus(StatusVoucherEnum.OVERDUE.getCode());
    if (vouchers.isEmpty()) {
      log.info("No expired vouchers.");
      return;
    }

    List<String> recipients = resolveNotificationRecipients();
    if (recipients.isEmpty()) {
      log.warn("There are {} expired vouchers, but no user has the VOUCHER_NOTIFICATION permission.", vouchers.size());
      return;
    }

    LocalDate today = LocalDate.now(ZoneId.of(TIME_ZONE));
    List<ExpiredVoucherWarningItem> items = vouchers.stream()
        .map(v -> new ExpiredVoucherWarningItem(
            v.getCode(), v.getClient().getName(), v.getPromoter().getName(), v.getVisitDate(),
            ChronoUnit.DAYS.between(v.getVisitDate(), today)))
        .toList();

    EmailSenderService.Message.MessageBuilder builder = EmailSenderService.Message.builder()
        .subject("Vouchers vencidos - " + vouchers.size() + (vouchers.size() == 1 ? " voucher" : " vouchers"))
        .template("voucher/warning-voucher-expired")
        .eventType("voucher_expired_warning")
        .data("vouchers", items)
        .data("company", companySettingsService.getOrDefaultModel());
    recipients.forEach(builder::to);
    emailSenderService.sendThymeleaf(builder.build());

    log.info("Warning e-mail sent for {} expired vouchers.", vouchers.size());
  }

  /** Degrada pra lista vazia em qualquer falha ao consultar o NimbusAuth (ex.: indisponibilidade
   *  temporária) - um job agendado não deve lançar exceção não tratada, só logar e pular o envio
   *  desta execução (a próxima tentativa é amanhã de qualquer forma). */
  private List<String> resolveNotificationRecipients() {
    try {
      return nimbusAuthInternalClient.fetchOptionsByPermission(NIMBUSNOVAX_APP_KEY, VOUCHER_NOTIFICATION_PERMISSION)
          .stream()
          .map(NimbusAuthInternalClient.UserSummary::username)
          .filter(email -> email != null && !email.isBlank())
          .toList();
    } catch (Exception e) {
      log.warn("Falha ao resolver destinatários de VOUCHER_NOTIFICATION no NimbusAuth: {}", e.getMessage());
      return List.of();
    }
  }

  @Scheduled(cron = "0 0 4 * * *", zone = TIME_ZONE)
  @Transactional
  public void expireConfirmedVouchers() {
    ConfigVoucher config = configVoucherService.getOrCreate();
    LocalDate limit = LocalDate.now().minusDays(config.getDaysToExpire());

    List<Voucher> vouchers = voucherRepository.findByVisitDateLessThanEqualAndStatusIn(
        limit, List.of(StatusVoucherEnum.CONFIRMED.getCode()));

    if (vouchers.isEmpty()) {
      log.info("No vouchers to mark as expired.");
      return;
    }

    vouchers.forEach(voucher -> {
      voucher.expire();
      log.info("Voucher {} marked as expired.", voucher.getCode());
    });
  }

  @Scheduled(cron = "0 10 4 * * *", zone = TIME_ZONE)
  @Transactional
  public void cancelOverdueDealingVouchers() {
    ConfigVoucher config = configVoucherService.getOrCreate();
    LocalDate limit = LocalDate.now().minusDays(config.getDaysToCancel());

    List<Voucher> vouchers = voucherRepository.findByVisitDateLessThanEqualAndStatusIn(
        limit, List.of(StatusVoucherEnum.DEALING.getCode()));

    if (vouchers.isEmpty()) {
      log.info("No vouchers to mark as canceled.");
      return;
    }

    CancellationReason systemReason = cancellationReasonRepository.findByName(SYSTEM_REASON_NAME).orElse(null);
    if (systemReason == null) {
      log.warn("Cancellation reason '{}' not found - skipping automatic cancellation.", SYSTEM_REASON_NAME);
      return;
    }

    vouchers.forEach(voucher -> {
      voucher.cancel(systemReason);
      log.info("Voucher {} marked as canceled.", voucher.getCode());
    });
  }
}
