package com.nimbusnovax.common.notification.mail;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSettingsRepository extends JpaRepository<EmailSettingsEntity, UUID> {

  /** Sem WHERE/ORDER BY de propósito - sempre há 0 ou 1 linha (ver EmailSettingsEntity). */
  Optional<EmailSettingsEntity> findFirstBy();
}
