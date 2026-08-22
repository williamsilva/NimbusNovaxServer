package com.nimbusnovax.common.company;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanySettingsRepository extends JpaRepository<CompanySettingsEntity, UUID> {

  Optional<CompanySettingsEntity> findFirstBy();
}
