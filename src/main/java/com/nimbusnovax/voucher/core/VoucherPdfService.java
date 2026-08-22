package com.nimbusnovax.voucher.core;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Gera o PDF do voucher a partir do template Thymeleaf {@code templates/voucher/voucher-pdf.html}
 * (HTML -> PDF via openhtmltopdf). O sistema legado (Novax antigo) usava JasperReports com um
 * template .jrxml compilado - HTML->PDF foi escolhido aqui por ser bem mais simples de manter e
 * evoluir sem exigir um template binário separado. O layout replica o voucher impresso do legado
 * (cabeçalho da empresa, dados do cliente/promotor, itens, total, informações importantes) - ver
 * {@link VoucherDocumentContext}.
 */
@Service
@RequiredArgsConstructor
public class VoucherPdfService {

  private final TemplateEngine templateEngine;

  byte[] renderPdf(VoucherDocumentContext doc) {
    Context context = new Context();
    context.setVariable("voucher", doc.voucher());
    context.setVariable("clientDocument", doc.clientDocument());
    context.setVariable("clientCity", doc.clientCity());
    context.setVariable("clientPhone", doc.clientPhone());
    context.setVariable("clientEmail", doc.clientEmail());
    context.setVariable("remainingValue", doc.remainingValue());
    context.setVariable("company", doc.company());
    context.setVariable("importantInfo", doc.importantInfo());
    String html = templateEngine.process("voucher/voucher-pdf", context);

    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(new InputSource(new StringReader(html)));

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.withW3cDocument(document, "");
      builder.toStream(out);
      builder.run();
      return out.toByteArray();
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to render voucher PDF", e);
    }
  }
}
