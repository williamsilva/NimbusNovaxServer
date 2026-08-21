package com.nimbusnovax.administracao.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
public enum CivilStateEnum {

  SINGLE(1),
  MARRIED(2),
  WIDOWED(3),
  DIVORCED(4);

  private final int code;

  CivilStateEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, CivilStateEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(CivilStateEnum::getCode, Function.identity()));

  public static CivilStateEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    CivilStateEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid CivilStateEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(CivilStateEnum civilState) {
    return civilState != null ? civilState.code : null;
  }
}
