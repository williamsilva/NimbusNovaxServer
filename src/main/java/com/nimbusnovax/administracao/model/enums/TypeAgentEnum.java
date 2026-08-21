package com.nimbusnovax.administracao.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/** Papel que um {@code Agent} pode assumir - um agente pode ter vários (ver AgentType). */
@Getter
public enum TypeAgentEnum {

  CLIENT(1),
  PROVIDER(2),
  PROMOTER(3),
  EMPLOYEE(4),
  TOUR_GUIDE(5);

  private final int code;

  TypeAgentEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, TypeAgentEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(TypeAgentEnum::getCode, Function.identity()));

  public static TypeAgentEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    TypeAgentEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid TypeAgentEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(TypeAgentEnum typeAgent) {
    return typeAgent != null ? typeAgent.code : null;
  }
}
