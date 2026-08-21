package com.nimbusnovax.administracao.repository;

import com.nimbusnovax.administracao.model.City;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, UUID> {

  List<City> findAllByStateIdOrderByNameAsc(UUID stateId);
}
