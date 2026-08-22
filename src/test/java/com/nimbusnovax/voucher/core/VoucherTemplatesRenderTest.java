package com.nimbusnovax.voucher.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import com.nimbusnovax.common.company.CompanySettingsModel;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
import com.nimbusnovax.voucher.model.enums.StatusVoucherEnum;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Sem contexto Spring de propósito (banco/segurança não são necessários pra isto) - só garante
 *  que os templates de e-mail/PDF de voucher (send-voucher.html/voucher-pdf.html) processam sem
 *  erro de expressão Thymeleaf com um voucher/empresa/informações importantes de exemplo, já que
 *  esse tipo de erro só aparece em runtime (compileJava não pega nada em arquivo .html). */
class VoucherTemplatesRenderTest {

  private TemplateEngine templateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");

    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);
    // Mesmo dialeto usado em produção (spring-boot-starter-thymeleaf autoconfigura
    // SpringStandardDialect, não o StandardDialect padrão) - troca ${...} de OGNL pra SpringEL,
    // o que também evita depender do jar do OGNL (não é dependência deste projeto).
    engine.setDialect(new SpringStandardDialect());
    return engine;
  }

  private VoucherResponse sampleVoucher(boolean withFoods, boolean withTourGuide) {
    VoucherResponse.AgentRefResponse client =
        new VoucherResponse.AgentRefResponse(UUID.randomUUID(), "Maurício da Silva Manhaes", "128.762.927-00");
    VoucherResponse.AgentRefResponse promoter =
        new VoucherResponse.AgentRefResponse(UUID.randomUUID(), "Ramonik Barreto", "000.000.000-00");
    VoucherResponse.AgentRefResponse tourGuide = withTourGuide
        ? new VoucherResponse.AgentRefResponse(UUID.randomUUID(), "Guia Teste", "111.111.111-11")
        : null;

    List<VoucherResponse.ItemResponse> tickets = List.of(
        new VoucherResponse.ItemResponse(
            UUID.randomUUID(), UUID.randomUUID(), "Grupo Excursão R$ 80,00", 41,
            new BigDecimal("80.00"), new BigDecimal("3280.00")));
    List<VoucherResponse.ItemResponse> foods = withFoods
        ? List.of(new VoucherResponse.ItemResponse(
            UUID.randomUUID(), UUID.randomUUID(), "Almoço", 5, new BigDecimal("35.00"), new BigDecimal("175.00")))
        : List.of();

    return new VoucherResponse(
        UUID.randomUUID(), "EVT1922", StatusVoucherEnum.DEALING, TypePersonEnum.PHYSICAL, null,
        LocalDate.of(2026, 1, 16), 2, new BigDecimal("3280.00"), new BigDecimal("1640.00"),
        new BigDecimal("3280.00"), withFoods ? new BigDecimal("175.00") : BigDecimal.ZERO,
        null, null, client, promoter, tourGuide, null, tickets, foods, Instant.now(), Instant.now());
  }

  private CompanySettingsModel sampleCompany() {
    return new CompanySettingsModel(
        "ACQUAMANIA MÚLTIPLO LAZER S.A", "39.303.847/0001-80",
        "Rua das Acácias, n° S/N - Comunidade Urbana de Lagoa Dourada", "Guarapari", "ES",
        "29226-766", "(27) 3221-6666", null);
  }

  private List<String> sampleImportantInfo() {
    return List.of(
        "É proibida a entrada de alimentos e bebidas no parque",
        "Remarcações serão permitidas até 24hs do dia anterior ao agendado para a visita");
  }

  private void assertRenders(String templateName, VoucherResponse voucher, CompanySettingsModel company,
      List<String> importantInfo) {
    Context context = new Context();
    context.setVariable("voucher", voucher);
    context.setVariable("clientCity", "Campos dos Goytacazes/RJ");
    context.setVariable("clientPhone", "(22) 9989-3273");
    context.setVariable("clientEmail", "mauriciocncb@outlook.com");
    context.setVariable("remainingValue", voucher.totalPrice().subtract(voucher.advanceValue()));
    context.setVariable("company", company);
    context.setVariable("importantInfo", importantInfo);
    context.setVariable("emailBody", "<p>Muito obrigado pela sua visita!</p>");

    String html = templateEngine().process(templateName, context);

    assertThat(html).isNotBlank();
    assertThat(html).contains(voucher.code());
    assertThat(html).contains(voucher.client().name());
  }

  @Test
  void rendersSendVoucherEmail_withFoodsAndTourGuideAndImportantInfo() {
    assertRenders("voucher/send-voucher", sampleVoucher(true, true), sampleCompany(), sampleImportantInfo());
  }

  @Test
  void rendersSendVoucherEmail_ticketsOnly_noCompanyNoImportantInfo() {
    // Empresa/config ainda não preenchidas (getOrDefaultModel/parseLines devolvem valores em
    // branco nesse caso, ver CompanySettingsService/VoucherFlowService) - não pode quebrar.
    assertRenders("voucher/send-voucher", sampleVoucher(false, false),
        new CompanySettingsModel(null, null, null, null, null, null, null, null), List.of());
  }

  @Test
  void rendersVoucherPdf_withFoodsAndTourGuideAndImportantInfo() {
    assertRenders("voucher/voucher-pdf", sampleVoucher(true, true), sampleCompany(), sampleImportantInfo());
  }

  @Test
  void rendersVoucherPdf_ticketsOnly_noCompanyNoImportantInfo() {
    assertRenders("voucher/voucher-pdf", sampleVoucher(false, false),
        new CompanySettingsModel(null, null, null, null, null, null, null, null), List.of());
  }

  @Test
  void rendersChangeVoucherEmail_withCompany() {
    assertRenders("voucher/change-voucher", sampleVoucher(false, false), sampleCompany(), List.of());
  }

  @Test
  void rendersChangeVoucherEmail_noCompany() {
    assertRenders("voucher/change-voucher", sampleVoucher(false, false),
        new CompanySettingsModel(null, null, null, null, null, null, null, null), List.of());
  }
}
