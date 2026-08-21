package com.nimbusnovax.voucher.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConfigVoucherRequest(
    Boolean senderMail,
    @NotNull @Min(1) Integer daysToExpire,
    @NotNull @Min(1) Integer daysToCancel,
    @NotNull @Min(1) Integer numberPendingVouchers,
    String emailBody,
    String notificationEmails) {
}
