package com.nimbusnovax.common.notification.mail;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
public class SmtpEmailSenderService implements EmailSenderService {

  private final EmailSettingsService emailSettingsService;
  private final EmailTemplateProcessor templateProcessor;
  private final EmailLogService emailLogService;

  @Override
  public void sendThymeleaf(Message message) {
    JavaMailSender mailSender = buildMailSender();
    // Renderizado fora do try/catch de envio em si, mas dentro do try geral - se o próprio
    // template falhar (ex.: variável faltando), body fica null e o log de erro registra isso
    // sem corpo, em vez de perder o registro inteiro.
    String body = null;
    try {
      body = templateProcessor.process(message);
      MimeMessage mimeMessage = createMimeMessage(message, mailSender, body);
      mailSender.send(mimeMessage);
      emailLogService.logSent(message, body);
    } catch (Exception e) {
      emailLogService.logError(message, body, e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "EMAIL_NOT_SENT", e);
    }
  }

  private JavaMailSender buildMailSender() {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(emailSettingsService.getSmtpHost());
    Integer port = emailSettingsService.getSmtpPort();
    sender.setPort(port != null ? port : 587);
    sender.setUsername(emailSettingsService.getSmtpUsername());
    sender.setPassword(emailSettingsService.getSmtpPassword());

    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", String.valueOf(Boolean.TRUE.equals(emailSettingsService.getSmtpAuth())));
    props.put("mail.smtp.starttls.enable", String.valueOf(Boolean.TRUE.equals(emailSettingsService.getSmtpStarttls())));
    props.put("mail.smtp.ssl.enable", String.valueOf(Boolean.TRUE.equals(emailSettingsService.getSmtpSsl())));
    // Sem isto, um host/porta bloqueado (ex.: provedor de hospedagem bloqueando a porta 587 de
    // saída) trava a conexão até o timeout default do SO (pode levar minutos) - sendThymeleaf() é
    // sempre chamado de dentro da mesma transação/requisição HTTP que salva a entidade de negócio
    // (Chamado/Aditivo/Pagamento), então travar aqui trava a requisição inteira, dando a impressão
    // de que o registro nem foi salvo (o proxy/navegador desiste antes do commit acontecer).
    props.put("mail.smtp.connectiontimeout", "5000");
    props.put("mail.smtp.timeout", "5000");
    props.put("mail.smtp.writetimeout", "5000");

    return sender;
  }

  private MimeMessage createMimeMessage(Message message, JavaMailSender mailSender, String body) throws Exception {
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    // MULTIPART_MODE_RELATED (não MIXED) - imagem referenciada via cid:/addInline precisa disso
    // pro cliente de e-mail resolver a referência (mesmo motivo documentado no NimbusAuth).
    MimeMessageHelper helper =
        new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_RELATED, "UTF-8");

    helper.setText(body, true);
    helper.setSubject(message.getSubject());
    helper.setFrom(emailSettingsService.getFromEmail());
    helper.setTo(message.getRecipients().toArray(new String[0]));

    if (message.getReplyTo() != null && !message.getReplyTo().isBlank()) {
      helper.setReplyTo(message.getReplyTo());
    }

    if (message.getInlines() != null) {
      for (EmailSenderService.InlineResource inline : message.getInlines()) {
        helper.addInline(inline.getContentId(), inline.getResource(), inline.getContentType());
      }
    }

    if (message.getAttachments() != null) {
      for (EmailSenderService.Attachment attachment : message.getAttachments()) {
        helper.addAttachment(attachment.getFilename(), attachment.getResource(), attachment.getContentType());
      }
    }

    return mimeMessage;
  }
}
