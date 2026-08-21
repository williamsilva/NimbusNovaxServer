package com.nimbusnovax.voucher.model;

import com.nimbusnovax.administracao.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Base comum de {@link Ticket} e {@link Food} - mesmo papel de VoucherItemBase no sistema
 *  legado (Novax antigo): item de voucher = produto + quantidade + preço unitário/total. */
@Getter
@Setter
@MappedSuperclass
public abstract class VoucherItem {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "total_price", precision = 10, scale = 2)
  private BigDecimal totalPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "voucher_id", nullable = false)
  private Voucher voucher;

  public void calculateTotalPrice() {
    BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
    int qty = quantity != null ? quantity : 0;
    this.totalPrice = price.multiply(BigDecimal.valueOf(qty));
  }
}
