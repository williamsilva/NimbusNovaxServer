package com.nimbusnovax.voucher.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VoucherDocumentFormatTest {

  @Test
  void formatsCpfFromRawDigits() {
    assertThat(VoucherDocumentFormat.document("12876292700")).isEqualTo("128.762.927-00");
  }

  @Test
  void formatsCpfAlreadyMasked_reformatsConsistently() {
    assertThat(VoucherDocumentFormat.document("128.762.927-00")).isEqualTo("128.762.927-00");
  }

  @Test
  void formatsCnpjFromRawDigits() {
    assertThat(VoucherDocumentFormat.document("39303847000180")).isEqualTo("39.303.847/0001-80");
  }

  @Test
  void formatsCnpjAlreadyMasked_reformatsConsistently() {
    assertThat(VoucherDocumentFormat.document("39.303.847/0001-80")).isEqualTo("39.303.847/0001-80");
  }

  @Test
  void document_unexpectedLength_returnsOriginalUnchanged() {
    assertThat(VoucherDocumentFormat.document("123")).isEqualTo("123");
  }

  @Test
  void document_null_returnsNull() {
    assertThat(VoucherDocumentFormat.document(null)).isNull();
  }

  @Test
  void formatsCellphoneFromRawDigits() {
    assertThat(VoucherDocumentFormat.phone("22999893273")).isEqualTo("(22) 99989-3273");
  }

  @Test
  void formatsLandlineFromRawDigits() {
    assertThat(VoucherDocumentFormat.phone("2732216666")).isEqualTo("(27) 3221-6666");
  }

  @Test
  void formatsCellphoneAlreadyMasked_reformatsConsistently() {
    assertThat(VoucherDocumentFormat.phone("(22) 99989-3273")).isEqualTo("(22) 99989-3273");
  }

  @Test
  void phone_unexpectedLength_returnsOriginalUnchanged() {
    assertThat(VoucherDocumentFormat.phone("12345")).isEqualTo("12345");
  }

  @Test
  void phone_null_returnsNull() {
    assertThat(VoucherDocumentFormat.phone(null)).isNull();
  }
}
