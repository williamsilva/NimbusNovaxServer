package com.nimbusnovax.common.notification.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Mesmo padrão do NimbusAuth/CardsyncServer (com.nimbus.auth.core.config.EmailProperties /
 * com.cardsync.core.config.EmailProperties) - config de envio de e-mail própria do NimbusNovax,
 * independente da do NimbusAuth (que só cobre login/reset de senha). Só a infraestrutura
 * (Impl/settings/tela) - nenhum evento de negócio dispara envio ainda (fica pra quando o
 * NotificationService/EmailNotificationSender descritos em package-info.java do pacote pai forem
 * implementados).
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties("nimbusnovax.email")
public class EmailProperties {

  private String fromName;
  private String fromEmail;

  private Impl impl = Impl.FAKE;
  private final Brevo brevo = new Brevo();
  private final Smtp smtp = new Smtp();

  @Getter
  @Setter
  public static class Brevo {
    private String apiKey;
    private String baseUrl;
    private Integer port = 587;
    private String username;
  }

  @Getter
  @Setter
  public static class Smtp {
    private String host;
    private Integer port = 587;
    private String username;
    private String password;
    private Boolean auth = true;
    private Boolean starttls = false;
    private Boolean ssl = false;
  }

  // Nome genérico "API_KEY" (não "BREVO") desde a primeira versão - mesmo rename feito depois
  // no NimbusAuth/CardSync (o seletor de modo não deveria ficar acoplado ao fornecedor), aqui já
  // nasce certo por não ter nenhum valor legado gravado em banco/env var pra migrar.
  public enum Impl {
    SMTP, FAKE, API_KEY
  }
}
