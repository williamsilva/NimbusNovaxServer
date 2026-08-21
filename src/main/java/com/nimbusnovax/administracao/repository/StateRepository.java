package com.nimbusnovax.administracao.repository;

import com.nimbusnovax.administracao.model.State;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, UUID> {

  List<State> findAllByOrderByNameAsc();
}
