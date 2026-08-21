package com.nimbusnovax.administracao.model;

import com.nimbusnovax.administracao.model.enums.GenerationEnum;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
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

/** {@code generation} é sempre forçado para USER no save (ver CancellationReasonService) - só as
 *  linhas seed (SYSTEM) ficam protegidas contra edição/exclusão, e só na UI. */
@Getter
@Setter
@Entity
@Table(name = "cancellation_reasons")
public class CancellationReason {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 20, unique = true)
  private String name;

  @Column(nullable = false)
  private Integer status = StatusEnum.ACTIVE.getCode();

  @Column(nullable = false)
  private Integer generation = GenerationEnum.USER.getCode();

  @Column(length = 150)
  private String description;

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

  public GenerationEnum getGenerationEnum() {
    return GenerationEnum.fromCode(generation);
  }

  public void setGenerationEnum(GenerationEnum generation) {
    this.generation = generation == null ? GenerationEnum.USER.getCode() : GenerationEnum.toCode(generation);
  }
}
