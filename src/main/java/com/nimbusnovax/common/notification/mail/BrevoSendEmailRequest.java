package com.nimbusnovax.common.notification.mail;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Espelha o wire format da API REST do Brevo (POST /smtp/email) - mesmo shape usado no
 *  NimbusAuth/CardsyncServer. */
@Getter
@Builder
public class BrevoSendEmailRequest {

  private Sender sender;
  private String subject;
  private ReplyTo replyTo;
  private List<Recipient> to;
  private String htmlContent;
  private List<Attachment> attachment;

  @Getter
  @AllArgsConstructor
  public static class Sender {
    private String name;
    private String email;
  }

  @Getter
  @AllArgsConstructor
  public static class Recipient {
    private String email;
    private String name;
  }

  @Getter
  @AllArgsConstructor
  public static class ReplyTo {
    private String email;
    private String name;
  }

  /** Nome dos campos ("content"/"name") é o formato exato exigido pela API do Brevo -
   *  content é o arquivo em base64, sem prefixo "data:...;base64,". */
  @Getter
  @AllArgsConstructor
  public static class Attachment {
    private String content;
    private String name;
  }
}
