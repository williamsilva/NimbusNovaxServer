package com.nimbusnovax.voucher.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Forma de pagamento de um {@link com.nimbusnovax.voucher.model.AdvancePayment} - conceito novo
 * (sem equivalente no legado, que só guardava um valor único de "Depósito" em
 * {@code Voucher.advanceValue}), por isso os códigos não precisam bater com nada migrado.
 */
@Getter
public enum PaymentMethodEnum {

  BANK_DEPOSIT(1),
  CREDIT_CARD(2),
  DEBIT_CARD(3),
  PIX(4),
  CASH(5),
  CHECK(6),
  BANK_SLIP(7),
  OTHER(8);

  private final int code;

  PaymentMethodEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, PaymentMethodEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(PaymentMethodEnum::getCode, Function.identity()));

  public static PaymentMethodEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    PaymentMethodEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid PaymentMethodEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(PaymentMethodEnum paymentMethod) {
    return paymentMethod != null ? paymentMethod.code : null;
  }
}
