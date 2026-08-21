package com.nimbusnovax.administracao.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/** Origem de um {@code CancellationReason} - USER (cadastrado manualmente) ou SYSTEM (seed,
 *  protegido contra edição/exclusão na UI). Save sempre força USER (ver CancellationReasonService). */
@Getter
public enum GenerationEnum {

  USER(1),
  SYSTEM(2);

  private final int code;

  GenerationEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, GenerationEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(GenerationEnum::getCode, Function.identity()));

  public static GenerationEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    GenerationEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid GenerationEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(GenerationEnum generation) {
    return generation != null ? generation.code : null;
  }
}
