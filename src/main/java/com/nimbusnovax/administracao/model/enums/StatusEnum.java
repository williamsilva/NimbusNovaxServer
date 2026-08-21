package com.nimbusnovax.administracao.model.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * Status genérico (ativo/inativo/bloqueado) reaproveitado por Agent (status por papel:
 * client/provider/promoter/employee/tourGuide), Product e CancellationReason - mesmo código 1/2/3
 * do sistema legado (Novax antigo). Código 0 (NULL/"sem papel") não é um membro daqui: nos campos
 * de status por papel do Agent, o próprio Agent mapeia 0 &lt;-&gt; ausência (null) na sua camada de
 * getter/setter, no mesmo espírito de StatusUserEnum no NimbusAuth.
 */
@Getter
public enum StatusEnum {

  ACTIVE(1),
  INACTIVE(2),
  BLOCKED(3);

  private final int code;

  StatusEnum(int code) {
    this.code = code;
  }

  private static final Map<Integer, StatusEnum> BY_CODE =
      Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(StatusEnum::getCode, Function.identity()));

  public static StatusEnum fromCode(Integer code) {
    if (code == null) {
      return null;
    }
    StatusEnum value = BY_CODE.get(code);
    if (value == null) {
      throw new IllegalArgumentException("Invalid StatusEnum code: " + code);
    }
    return value;
  }

  public static Integer toCode(StatusEnum status) {
    return status != null ? status.code : null;
  }
}
