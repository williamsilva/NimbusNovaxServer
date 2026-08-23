package com.nimbusnovax.administracao.repository;

import com.nimbusnovax.administracao.model.CancellationReason;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CancellationReasonRepository
    extends JpaRepository<CancellationReason, UUID>, JpaSpecificationExecutor<CancellationReason> {

  Optional<CancellationReason> findByName(String name);
}
