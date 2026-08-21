package com.nimbusnovax.administracao.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum TypePersonEnum {

  PHYSICAL(1),
  LEGAL(2);

  private final int code;

  TypePersonEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, TypePersonEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(TypePersonEnum::getCode, Function.identity()));

  public static TypePersonEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    TypePersonEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid TypePersonEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(TypePersonEnum typePerson) {
    return typePerson != null ? typePerson.code : null;
  }
}
