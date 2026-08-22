package com.nimbusnovax.common.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Linha única (mesmo padrão de EmailSettingsEntity - EmailSettingsRepository.findFirstBy() sempre
 *  lê/atualiza a mesma linha, nunca há mais de uma) com os dados da empresa emissora do voucher
 *  (nome/CNPJ/endereço/telefone) - usados no cabeçalho do e-mail e do PDF enviados ao cliente (ver
 *  VoucherFlowService.buildDocumentContext), configuráveis em Configurações &gt; Empresa. */
@Getter
@Setter
@Entity
@Table(name = "company_settings")
public class CompanySettingsEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(length = 255)
  private String name;

  @Column(length = 20)
  private String document;

  @Column(name = "address_line", length = 255)
  private String addressLine;

  @Column(length = 100)
  private String city;

  @Column(length = 2)
  private String state;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Column(length = 20)
  private String phone;

  @Column(length = 255)
  private String email;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "created_by_id")
  private UUID createdById;

  @Column(name = "updated_by_id")
  private UUID updatedById;
}
