package com.nimbusnovax.voucher.dto.response;

import java.time.Instant;
import java.util.UUID;

public record ConfigVoucherResponse(
    UUID id,
    Boolean senderMail,
    Integer daysToExpire,
    Integer daysToCancel,
    Integer numberPendingVouchers,
    String emailBody,
    String notificationEmails,
    Instant createdAt,
    Instant updatedAt) {
}
