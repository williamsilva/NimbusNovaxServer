package com.nimbusnovax.administracao.model;

import com.nimbusnovax.administracao.model.enums.CivilStateEnum;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Papéis (Client/Provider/Promoter/Employee/TourGuide - ver {@link AgentType}), endereços e
 * contatos são coleções filhas com cascade+orphanRemoval - o service substitui a coleção inteira
 * (clear + re-add) em vez de fazer merge item a item, mesmo padrão do sistema legado
 * (AgentService.save recebia a árvore completa a cada save).
 */
@Getter
@Setter
@Entity
@Table(name = "agents")
public class Agent {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 20, unique = true)
  private String code;

  @Column(nullable = false, length = 50, unique = true)
  private String name;

  @Column(name = "social_reason", length = 50)
  private String socialReason;

  @Column(nullable = false, length = 20, unique = true)
  private String document;

  @Column(name = "is_attendant", nullable = false)
  private boolean attendant;

  @Column(name = "is_manager", nullable = false)
  private boolean manager;

  @Column(name = "status_client", nullable = false)
  private Integer statusClient = 0;

  @Column(name = "status_provider", nullable = false)
  private Integer statusProvider = 0;

  @Column(name = "status_promoter", nullable = false)
  private Integer statusPromoter = 0;

  @Column(name = "status_employee", nullable = false)
  private Integer statusEmployee = 0;

  @Column(name = "status_tour_guide", nullable = false)
  private Integer statusTourGuide = 0;

  @Column(length = 1)
  private String sex;

  @Column(length = 20)
  private String rg;

  @Column(name = "type_person", nullable = false)
  private Integer typePerson = 0;

  @Column(name = "civil_state")
  private Integer civilState = 0;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AgentType> agentTypes = new ArrayList<>();

  @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AgentAddress> addresses = new ArrayList<>();

  @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AgentContact> contacts = new ArrayList<>();

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

  public TypePersonEnum getTypePersonEnum() {
    return typePerson == null || typePerson == 0 ? null : TypePersonEnum.fromCode(typePerson);
  }

  public void setTypePersonEnum(TypePersonEnum typePerson) {
    this.typePerson = typePerson == null ? 0 : TypePersonEnum.toCode(typePerson);
  }

  public CivilStateEnum getCivilStateEnum() {
    return civilState == null || civilState == 0 ? null : CivilStateEnum.fromCode(civilState);
  }

  public void setCivilStateEnum(CivilStateEnum civilState) {
    this.civilState = civilState == null ? 0 : CivilStateEnum.toCode(civilState);
  }

  public StatusEnum getStatusClientEnum() {
    return statusClient == null || statusClient == 0 ? null : StatusEnum.fromCode(statusClient);
  }

  public void setStatusClientEnum(StatusEnum status) {
    this.statusClient = status == null ? 0 : StatusEnum.toCode(status);
  }

  public StatusEnum getStatusProviderEnum() {
    return statusProvider == null || statusProvider == 0 ? null : StatusEnum.fromCode(statusProvider);
  }

  public void setStatusProviderEnum(StatusEnum status) {
    this.statusProvider = status == null ? 0 : StatusEnum.toCode(status);
  }

  public StatusEnum getStatusPromoterEnum() {
    return statusPromoter == null || statusPromoter == 0 ? null : StatusEnum.fromCode(statusPromoter);
  }

  public void setStatusPromoterEnum(StatusEnum status) {
    this.statusPromoter = status == null ? 0 : StatusEnum.toCode(status);
  }

  public StatusEnum getStatusEmployeeEnum() {
    return statusEmployee == null || statusEmployee == 0 ? null : StatusEnum.fromCode(statusEmployee);
  }

  public void setStatusEmployeeEnum(StatusEnum status) {
    this.statusEmployee = status == null ? 0 : StatusEnum.toCode(status);
  }

  public StatusEnum getStatusTourGuideEnum() {
    return statusTourGuide == null || statusTourGuide == 0 ? null : StatusEnum.fromCode(statusTourGuide);
  }

  public void setStatusTourGuideEnum(StatusEnum status) {
    this.statusTourGuide = status == null ? 0 : StatusEnum.toCode(status);
  }
}
