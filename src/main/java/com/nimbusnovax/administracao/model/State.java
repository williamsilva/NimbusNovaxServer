package com.nimbusnovax.administracao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Dados de referência (UF) - só leitura pela API, usado no select de Endereço do Agente. */
@Getter
@Setter
@Entity
@Table(name = "states")
public class State {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, length = 3, unique = true)
  private String uf;
}
