package com.nimbusnovax.voucher.core;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.nimbusnovax.voucher.dto.response.VoucherResponse;
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
 * evoluir sem exigir um template binário separado; o layout replica as mesmas informações
 * (cliente/promotor/data de visita/itens/total), sem tentar imitar pixel a pixel o antigo.
 */
@Service
@RequiredArgsConstructor
public class VoucherPdfService {

  private final TemplateEngine templateEngine;

  public byte[] renderPdf(VoucherResponse voucher) {
    Context context = new Context();
    context.setVariable("voucher", voucher);
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
