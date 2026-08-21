package com.nimbusnovax.voucher.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Ingresso vendido no voucher - sem comportamento próprio, ver {@link VoucherItem}. */
@Entity
@Table(name = "voucher_tickets")
public class Ticket extends VoucherItem {
}
