package com.nimbusnovax.voucher.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VoucherCancellationRequest(@NotNull UUID cancellationReasonId) {
}
