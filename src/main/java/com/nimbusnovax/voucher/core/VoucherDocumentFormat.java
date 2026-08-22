package com.nimbusnovax.voucher.core;

/** Formatação de documento/telefone pro e-mail/PDF de voucher (ver
 *  VoucherFlowService.buildDocumentContext) - sempre reformata a partir dos dígitos, nunca confia
 *  no formato já salvo no Agente: o cadastro novo aplica máscara ao digitar, mas dados migrados do
 *  sistema legado podem ter vindo só com dígitos (ou com pontuação diferente), então o mesmo
 *  voucher não pode depender de qual dos dois casos o Agente caiu em. */
final class VoucherDocumentFormat {

  private VoucherDocumentFormat() {
  }

  /** CPF (000.000.000-00, 11 dígitos) ou CNPJ (00.000.000/0000-00, 14 dígitos). Comprimento
   *  diferente disso (cadastro incompleto/inválido) devolve o valor original sem mexer, em vez de
   *  produzir um formato quebrado. */
  static String document(String raw) {
    if (raw == null) {
      return null;
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.length() == 11) {
      return digits.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }
    if (digits.length() == 14) {
      return digits.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }
    return raw;
  }

  /** (00) 0000-0000 (fixo, 10 dígitos) ou (00) 00000-0000 (celular, 11 dígitos). Mesmo critério de
   *  {@link #document} pra comprimento inesperado. */
  static String phone(String raw) {
    if (raw == null) {
      return null;
    }
    String digits = raw.replaceAll("\\D", "");
    if (digits.length() == 11) {
      return digits.replaceAll("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
    }
    if (digits.length() == 10) {
      return digits.replaceAll("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
    }
    return raw;
  }
}
