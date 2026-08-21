package com.nimbusnovax.common.notification.mail;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.ToString;
import org.springframework.core.io.Resource;

/**
 * Mesmo papel do EmailSenderService no NimbusAuth/CardsyncServer, com duas diferenças de
 * propósito: só Thymeleaf (nenhum template Freemarker existe ou está planejado aqui - as duas
 * outras apps só mantêm o método por herança de código pré-multi-app) e {@code requestedById} é
 * String (não UUID de um UserRepository local - o NimbusNovaxServer não tem tabela própria de
 * usuário, identidade vem 100% do NimbusAuth via JWT/sessão).
 */
public interface EmailSenderService {

  void sendThymeleaf(Message message);

  @Getter
  @Builder
  @ToString
  class Message {

    private String replyTo;

    @Singular("to")
    private Set<String> recipients;

    @NonNull
    private String subject;

    @NonNull
    private String template;

    private Locale locale;

    /** id (sub do JWT) de quem disparou o envio, se houver - só pra auditoria em EmailLog. */
    private String requestedById;

    /** Rótulo livre (ex.: "addendum_approved") - sem enum fixo ainda, nenhum evento de negócio
     *  concreto foi definido nesta versão. */
    @NonNull
    private String eventType;

    @Singular("data")
    private Map<String, Object> data;

    @Singular("inline")
    private Set<InlineResource> inlines;

    /** Anexo de arquivo de verdade (ex.: PDF de ordem de pagamento) - diferente de
     *  {@code inlines}, que é só pra imagem referenciada via {@code cid:} dentro do próprio
     *  HTML do corpo. */
    @Singular("attachment")
    private Set<Attachment> attachments;
  }

  @Getter
  @Builder
  @ToString
  class InlineResource {

    @NonNull
    private String contentId;

    @NonNull
    private Resource resource;

    @NonNull
    private String contentType;
  }

  @Getter
  @Builder
  @ToString
  class Attachment {

    @NonNull
    private String filename;

    @NonNull
    private Resource resource;

    @NonNull
    private String contentType;
  }
}
