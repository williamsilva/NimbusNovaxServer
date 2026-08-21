package com.nimbusnovax.voucher.model;

import com.nimbusnovax.administracao.model.Agent;
import com.nimbusnovax.administracao.model.CancellationReason;
import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Máquina de estados replicada do {@code Voucher} do sistema legado (Novax antigo): nasce DEALING,
 * e só pode mudar de estado enquanto não estiver EXCHANGED nem CALLED_OFF ({@link #canChangeStatus}
 * é a mesma "defesa em profundidade" do domínio antigo - o {@code VoucherService}/
 * {@code VoucherFlowService} novos repetem essas mesmas checagens antes de chamar estes métodos,
 * mas a entidade não confia neles e se protege de qualquer forma).
 */
@Getter
@Setter
@Entity
@Table(name = "vouchers")
public class Voucher {

  private static final BigDecimal ZERO = BigDecimal.ZERO;

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false, length = 10, unique = true)
  private String code;

  @Column(nullable = false)
  private Integer status;

  @Column(name = "type_person", nullable = false)
  private Integer typePerson;

  @Column(name = "number_of_visit")
  private Integer numberOfVisit;

  @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalPrice = ZERO;

  @Column(name = "advance_value", nullable = false, precision = 10, scale = 2)
  private BigDecimal advanceValue = ZERO;

  @Column(name = "total_price_tickets", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalPriceTickets = ZERO;

  @Column(name = "total_price_foods", nullable = false, precision = 10, scale = 2)
  private BigDecimal totalPriceFoods = ZERO;

  @Column(name = "visit_date")
  private LocalDate visitDate;

  @Column(name = "confirmation_date")
  private Instant confirmationDate;

  @Column(name = "cancellation_date")
  private Instant cancellationDate;

  @Column(length = 200)
  private String note;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "client_id", nullable = false)
  private Agent client;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promoter_id", nullable = false)
  private Agent promoter;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tour_guide_id")
  private Agent tourGuide;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cancellation_reason_id")
  private CancellationReason cancellationReason;

  @OrderBy("id")
  @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Ticket> tickets = new ArrayList<>();

  @OrderBy("id")
  @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Food> foods = new ArrayList<>();

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

  public StatusVoucherEnum getStatusEnum() {
    return StatusVoucherEnum.fromCode(status);
  }

  public void setStatusEnum(StatusVoucherEnum status) {
    this.status = StatusVoucherEnum.toCode(status);
  }

  public TypePersonEnum getTypePersonEnum() {
    return TypePersonEnum.fromCode(typePerson);
  }

  public void setTypePersonEnum(TypePersonEnum typePerson) {
    this.typePerson = TypePersonEnum.toCode(typePerson);
  }

  public void expire() {
    if (canChangeStatus()) {
      setStatusEnum(StatusVoucherEnum.OVERDUE);
    }
  }

  public void confirm() {
    if (canChangeStatus()) {
      setStatusEnum(StatusVoucherEnum.CONFIRMED);
      setConfirmationDate(Instant.now());
    }
  }

  public void notConfirm() {
    if (canChangeStatus()) {
      setStatusEnum(StatusVoucherEnum.NOT_CLOSED);
    }
  }

  public void change() {
    if (canChangeStatus()) {
      setStatusEnum(StatusVoucherEnum.EXCHANGED);
    }
  }

  public void cancel(CancellationReason reason) {
    if (canChangeStatus()) {
      setStatusEnum(StatusVoucherEnum.CALLED_OFF);
      setCancellationDate(Instant.now());
      setCancellationReason(reason);
    }
  }

  /** Um voucher trocado/cancelado é definitivo - nenhuma transição de status feita a partir daqui
   *  jamais tem efeito, nem a mudança silenciosa de campos via update. */
  private boolean canChangeStatus() {
    StatusVoucherEnum current = getStatusEnum();
    return current != StatusVoucherEnum.EXCHANGED && current != StatusVoucherEnum.CALLED_OFF;
  }

  /** Usada pelo {@code VoucherService} pra decidir se um PUT genérico ainda pode alterar dados do
   *  voucher (fluxo de status usa os métodos acima, que têm sua própria defesa). */
  public boolean canBeModified() {
    return EnumSet.of(StatusVoucherEnum.DEALING, StatusVoucherEnum.OVERDUE, StatusVoucherEnum.CONFIRMED)
        .contains(getStatusEnum());
  }

  @PrePersist
  @PreUpdate
  private void nonNullBigDecimals() {
    totalPrice = Optional.ofNullable(totalPrice).orElse(ZERO);
    advanceValue = Optional.ofNullable(advanceValue).orElse(ZERO);
    totalPriceTickets = Optional.ofNullable(totalPriceTickets).orElse(ZERO);
    totalPriceFoods = Optional.ofNullable(totalPriceFoods).orElse(ZERO);
  }

  public void calculateTotalPrice() {
    tickets.forEach(Ticket::calculateTotalPrice);
    totalPriceTickets = tickets.stream().map(Ticket::getTotalPrice).reduce(ZERO, BigDecimal::add);

    foods.forEach(Food::calculateTotalPrice);
    totalPriceFoods = foods.stream().map(Food::getTotalPrice).reduce(ZERO, BigDecimal::add);

    totalPrice = totalPriceTickets.add(totalPriceFoods);
  }
}
