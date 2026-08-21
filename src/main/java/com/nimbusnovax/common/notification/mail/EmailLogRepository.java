package com.nimbusnovax.common.notification.mail;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLogEntity, UUID> {
}
