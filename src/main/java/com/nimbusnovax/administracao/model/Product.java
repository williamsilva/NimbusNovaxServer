package com.nimbusnovax.administracao.model;

import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeProductEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Entity
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 100, unique = true)
  private String name;

  @Column(nullable = false)
  private Integer status = StatusEnum.ACTIVE.getCode();

  @Column(name = "type_product", nullable = false)
  private Integer typeProduct;

  @Column(length = 100)
  private String description;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @Column(name = "initial_validate")
  private LocalDate initialValidate;

  @Column(name = "final_validate")
  private LocalDate finalValidate;

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

  public StatusEnum getStatusEnum() {
    return StatusEnum.fromCode(status);
  }

  public void setStatusEnum(StatusEnum status) {
    this.status = status == null ? StatusEnum.ACTIVE.getCode() : StatusEnum.toCode(status);
  }

  public TypeProductEnum getTypeProductEnum() {
    return TypeProductEnum.fromCode(typeProduct);
  }

  public void setTypeProductEnum(TypeProductEnum typeProduct) {
    this.typeProduct = TypeProductEnum.toCode(typeProduct);
  }
}
