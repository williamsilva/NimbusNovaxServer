package com.nimbusnovax.common.notification.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Só loga - usado em dev (default) pra não precisar de credencial nenhuma. Também audita em
 *  EmailLog (igual às demais implementações) - antes não auditava, deixando a tela de Auditoria de
 *  E-mail vazia sempre que o ambiente estivesse em modo FAKE. */
@Slf4j
@RequiredArgsConstructor
public class FakeEmailSenderService implements EmailSenderService {

  private final EmailTemplateProcessor templateProcessor;
  private final EmailLogService emailLogService;

  @Override
  public void sendThymeleaf(Message message) {
    String body = templateProcessor.process(message);

    log.info("[SEND FAKE E-MAIL] ReplyTo: {}", message.getReplyTo());
    log.info("[SEND FAKE E-MAIL] To: {}\n{}", message.getRecipients(), body);
    if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
      log.info("[SEND FAKE E-MAIL] Attachments: {}", message.getAttachments().stream()
          .map(EmailSenderService.Attachment::getFilename).toList());
    }

    emailLogService.logSent(message, body);
  }
}
