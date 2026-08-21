package com.nimbusnovax.voucher.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Item de alimentação vendido no voucher - sem comportamento próprio, ver {@link VoucherItem}. */
@Entity
@Table(name = "voucher_foods")
public class Food extends VoucherItem {
}
