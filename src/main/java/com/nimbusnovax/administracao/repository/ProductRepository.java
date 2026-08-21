package com.nimbusnovax.administracao.repository;

import com.nimbusnovax.administracao.model.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {

  Optional<Product> findByName(String name);
}
