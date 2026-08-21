package com.nimbusnovax.voucher.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Mesmos códigos numéricos do sistema legado (Novax antigo) - preservados de propósito pra bater
 * 1:1 com os dados migrados de {@code ws_voucher.status} sem precisar de tradução na migration de
 * dados. NULL(0) do legado não é um membro aqui: um Voucher sempre nasce DEALING (ver
 * VoucherService.create), então "sem status" nunca é um estado válido de um voucher persistido.
 */
@Getter
public enum StatusVoucherEnum {

  DEALING(1),
  CONFIRMED(2),
  EXCHANGED(3),
  OVERDUE(4),
  CALLED_OFF(5),
  NOT_CLOSED(6);

  private final int code;

  StatusVoucherEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, StatusVoucherEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(StatusVoucherEnum::getCode, Function.identity()));

  public static StatusVoucherEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    StatusVoucherEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid StatusVoucherEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(StatusVoucherEnum status) {
    return status != null ? status.code : null;
  }
}
