package com.nimbusnovax.voucher.model;

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

/** Linha única de configuração de voucher (chave fixa "VOUCHER_CHANGE", ver
 *  {@code ConfigVoucherService}) - mesmo papel de {@code ws_config_voucher} no sistema legado.
 *  O campo {@code key} foi mantido (em vez de assumir singleton sem chave) só pra não perder a
 *  possibilidade de outras configs no futuro, igual ao antigo. */
@Getter
@Setter
@Entity
@Table(name = "voucher_configs")
public class ConfigVoucher {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "config_key", nullable = false, unique = true, length = 50)
  private String key;

  @Column(name = "sender_mail", nullable = false)
  private Boolean senderMail = Boolean.TRUE;

  @Column(name = "days_to_expire", nullable = false)
  private Integer daysToExpire;

  @Column(name = "days_to_cancel", nullable = false)
  private Integer daysToCancel;

  @Column(name = "number_pending_vouchers", nullable = false)
  private Integer numberPendingVouchers;

  @Column(name = "email_body", columnDefinition = "text")
  private String emailBody;

  /** Divergência deliberada do sistema legado: lá os destinatários do aviso de vouchers vencidos
   *  eram os usuários com uma permissão especial (WsSecurity.ROLE_VOUCHER_NOTIFICATION) - aqui não
   *  há tabela de usuário local (identidade vem 100% do NimbusAuth via JWT), então a lista de
   *  e-mails fica configurada diretamente aqui (separada por vírgula). */
  @Column(name = "notification_emails", length = 500)
  private String notificationEmails;

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
