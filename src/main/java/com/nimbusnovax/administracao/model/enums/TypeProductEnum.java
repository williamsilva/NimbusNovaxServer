package com.nimbusnovax.administracao.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum TypeProductEnum {

  TICKET(1),
  FOOD(2),
  COURTESY(3);

  private final int code;

  TypeProductEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, TypeProductEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(TypeProductEnum::getCode, Function.identity()));

  public static TypeProductEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    TypeProductEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid TypeProductEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(TypeProductEnum typeProduct) {
    return typeProduct != null ? typeProduct.code : null;
  }
}
