package com.nimbusnovax.administracao.repository;

import com.nimbusnovax.administracao.model.CancellationReason;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationReasonRepository extends JpaRepository<CancellationReason, UUID> {

  Optional<CancellationReason> findByName(String name);
}
