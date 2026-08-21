package com.nimbusnovax.administracao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Dados de referência (cidade/IBGE) - só leitura pela API, usado no select de Endereço do Agente. */
@Getter
@Setter
@Entity
@Table(name = "cities")
public class City {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 50)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "state_id", nullable = false)
  private State state;
}
