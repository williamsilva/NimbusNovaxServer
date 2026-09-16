package com.nimbusnovax.voucher.model;

import com.nimbusnovax.voucher.model.enums.PaymentMethodEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Uma parcela do pagamento antecipado do voucher (ex-"Depósito"), agora com forma de pagamento -
 *  mesmo papel de {@link VoucherItem} (produto + quantidade) só que sem produto, uma forma de
 *  pagamento + valor. */
@Getter
@Setter
@Entity
@Table(name = "voucher_advance_payments")
public class AdvancePayment {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "payment_method", nullable = false)
  private Integer paymentMethod;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal amount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "voucher_id", nullable = false)
  private Voucher voucher;

  public PaymentMethodEnum getPaymentMethodEnum() {
    return PaymentMethodEnum.fromCode(paymentMethod);
  }

  public void setPaymentMethodEnum(PaymentMethodEnum paymentMethod) {
    this.paymentMethod = PaymentMethodEnum.toCode(paymentMethod);
  }
}
