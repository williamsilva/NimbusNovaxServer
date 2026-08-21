package com.nimbusnovax.common.notification.mail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Implementação concreta do modo API_KEY - continua se chamando "Brevo" (é o fornecedor de
 *  fato usado por trás), mesma decisão já tomada no NimbusAuth/CardsyncServer: o nome do MODO
 *  (Impl.API_KEY) é genérico, o nome da CLASSE que implementa esse modo é específico do
 *  fornecedor - trocar de fornecedor no futuro significa trocar esta classe, não o seletor. */
@RequiredArgsConstructor
public class BrevoEmailSenderService implements EmailSenderService {

  private final RestClient restClient;
  private final EmailSettingsService emailSettingsService;
  private final EmailLogService emailLogService;
  private final EmailTemplateProcessor templateProcessor;

  @Override
  public void sendThymeleaf(Message message) {
    String htmlBody = null;
    try {
      htmlBody = templateProcessor.process(message);

      BrevoSendEmailRequest payload = BrevoSendEmailRequest.builder()
          .sender(new BrevoSendEmailRequest.Sender(
              emailSettingsService.getFromName(), emailSettingsService.getFromEmail()))
          .to(message.getRecipients().stream()
              .map(email -> new BrevoSendEmailRequest.Recipient(email, null))
              .toList())
          .replyTo(message.getReplyTo() != null && !message.getReplyTo().isBlank()
              ? new BrevoSendEmailRequest.ReplyTo(message.getReplyTo(), null)
              : null)
          .subject(message.getSubject())
          .htmlContent(htmlBody)
          .attachment(toBrevoAttachments(message))
          .build();

      restClient.post()
          .uri("/smtp/email")
          .header("api-key", emailSettingsService.getBrevoApiKey())
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();

      emailLogService.logSent(message, htmlBody);
    } catch (Exception e) {
      emailLogService.logError(message, htmlBody, e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Could not send email via provider", e);
    }
  }

  /** Brevo recebe anexo direto no corpo da requisição (base64, sem multipart) - null (não lista
   *  vazia) quando não há anexo, pra não mandar "attachment": [] à toa. */
  private java.util.List<BrevoSendEmailRequest.Attachment> toBrevoAttachments(Message message) {
    if (message.getAttachments() == null || message.getAttachments().isEmpty()) {
      return null;
    }

    return message.getAttachments().stream()
        .map(att -> new BrevoSendEmailRequest.Attachment(encodeBase64(att), att.getFilename()))
        .toList();
  }

  private String encodeBase64(EmailSenderService.Attachment attachment) {
    try {
      return Base64.getEncoder().encodeToString(attachment.getResource().getContentAsByteArray());
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read attachment resource: " + attachment.getFilename(), e);
    }
  }
}
