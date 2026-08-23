package com.nimbusnovax.common.notification.mail;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmailLogRepository
    extends JpaRepository<EmailLogEntity, UUID>, JpaSpecificationExecutor<EmailLogEntity> {
}
