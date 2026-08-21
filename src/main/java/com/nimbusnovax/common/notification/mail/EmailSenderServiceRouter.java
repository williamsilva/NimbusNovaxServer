package com.nimbusnovax.common.notification.mail;

import java.net.http.HttpClient;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Único bean Spring do tipo EmailSenderService - resolve a implementação de novo (banco, via
 * EmailSettingsService.getImpl(), cacheado e invalidado no update()) A CADA envio, em vez de
 * escolher uma implementação fixa no boot como antes (EmailConfig, removido). Isso é o que faz a
 * troca SMTP/API_KEY/FAKE na tela "Configurações &gt; E-mail" valer imediatamente, sem precisar
 * reiniciar o processo - antes, salvar um impl novo na tela não tinha efeito nenhum até o próximo
 * deploy/restart, porque o @Bean só era construído uma vez.
 *
 * <p>Reconstruir Fake/Smtp/Brevo a cada chamada é barato (nenhum guarda estado/conexão pooled -
 * SmtpEmailSenderService já reconstruía o JavaMailSender a cada envio mesmo antes desta classe
 * existir) e evita qualquer lógica extra de invalidação além da que EmailSettingsService já tem;
 * em especial, a baseUrl do RestClient do Brevo também precisa ser lida de novo a cada chamada
 * (não só o impl), senão uma troca de brevoBaseUrl na tela sofreria o mesmo problema de ficar
 * "presa" até reiniciar.
 */
@Service
@RequiredArgsConstructor
public class EmailSenderServiceRouter implements EmailSenderService {

  private final EmailSettingsService emailSettingsService;
  private final EmailLogService emailLogService;
  private final RestClient.Builder restClientBuilder;
  private final EmailTemplateProcessor templateProcessor;

  @Override
  public void sendThymeleaf(Message message) {
    delegate().sendThymeleaf(message);
  }

  private EmailSenderService delegate() {
    return switch (emailSettingsService.getImpl()) {
      case FAKE -> new FakeEmailSenderService(templateProcessor, emailLogService);
      case SMTP -> new SmtpEmailSenderService(emailSettingsService, templateProcessor, emailLogService);
      case API_KEY -> new BrevoEmailSenderService(
          restClientBuilder
              .baseUrl(emailSettingsService.getBrevoBaseUrl())
              .requestFactory(httpTimeouts())
              .build(),
          emailSettingsService,
          emailLogService,
          templateProcessor);
    };
  }

  /** Sem isto, um host/rede bloqueada (proxy/firewall do provedor de hospedagem) trava a chamada
   *  HTTP até o timeout default do cliente - sendThymeleaf() é sempre chamado de dentro da mesma
   *  transação/requisição HTTP que salva a entidade de negócio (Chamado/Aditivo/Pagamento), então
   *  travar aqui trava a requisição inteira (mesmo racional do timeout em
   *  SmtpEmailSenderService#buildMailSender). JdkClientHttpRequestFactory (core do Spring
   *  Framework) em vez das classes de conveniência do Spring Boot pra isso - essas mudaram de
   *  pacote entre a versão usada aqui (3.3.2) e a do CardsyncServer (4.0.2), então usar a API do
   *  framework direto evita divergência entre os dois. */
  private static ClientHttpRequestFactory httpTimeouts() {
    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(10));
    return factory;
  }
}
