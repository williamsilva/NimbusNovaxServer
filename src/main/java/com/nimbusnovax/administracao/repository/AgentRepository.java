package com.nimbusnovax.administracao.repository;

import com.nimbusnovax.administracao.model.Agent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

  Optional<Agent> findByName(String name);

  Optional<Agent> findByDocument(String document);

  /** Maior código numérico já usado (base pro próximo "1000+"); ignora eventuais códigos não
   *  numéricos pra não quebrar o CAST. */
  @Query(value = "SELECT MAX(CAST(code AS integer)) FROM agents WHERE code ~ '^[0-9]+$'", nativeQuery = true)
  Integer findMaxNumericCode();
}
