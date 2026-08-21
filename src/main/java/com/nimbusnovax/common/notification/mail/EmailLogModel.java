package com.nimbusnovax.common.notification.mail;

import java.time.Instant;
import java.util.UUID;

public record EmailLogModel(
    UUID id,
    String eventType,
    String recipients,
    String subject,
    String template,
    String body,
    EmailLogStatus status,
    String errorMessage,
    String requestedById,
    Instant sentAt) {
}
