package com.nimbusnovax.common.notification.mail;

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

/** Linha única (mesmo padrão de EmailSettingsEntity no NimbusAuth/CardsyncServer -
 *  EmailSettingsRepository.findFirstBy() sempre lê/atualiza a mesma linha, nunca há mais de uma). */
@Getter
@Setter
@Entity
@Table(name = "email_settings")
public class EmailSettingsEntity {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(length = 10)
  private String impl;

  @Column(name = "from_name", length = 255)
  private String fromName;

  @Column(name = "from_email", length = 255)
  private String fromEmail;

  @Column(name = "brevo_api_key", length = 500)
  private String brevoApiKey;

  @Column(name = "brevo_base_url", length = 255)
  private String brevoBaseUrl;

  @Column(name = "brevo_port")
  private Integer brevoPort;

  @Column(name = "brevo_username", length = 255)
  private String brevoUsername;

  @Column(name = "smtp_host", length = 255)
  private String smtpHost;

  @Column(name = "smtp_port")
  private Integer smtpPort;

  @Column(name = "smtp_username", length = 255)
  private String smtpUsername;

  @Column(name = "smtp_password", length = 500)
  private String smtpPassword;

  @Column(name = "smtp_auth")
  private Boolean smtpAuth;

  @Column(name = "smtp_starttls")
  private Boolean smtpStarttls;

  @Column(name = "smtp_ssl")
  private Boolean smtpSsl;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
}
