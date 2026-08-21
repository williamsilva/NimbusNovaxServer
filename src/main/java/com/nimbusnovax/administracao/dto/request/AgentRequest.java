package com.nimbusnovax.administracao.dto.request;

import com.nimbusnovax.administracao.model.enums.CivilStateEnum;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeAgentEnum;
import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AgentRequest(
    @NotBlank String name,
    String socialReason,
    @NotBlank String document,
    String rg,
    String sex,
    TypePersonEnum typePerson,
    CivilStateEnum civilState,
    LocalDate birthDate,
    boolean isManager,
    boolean isAttendant,
    List<TypeAgentEnum> roles,
    StatusEnum statusClient,
    StatusEnum statusProvider,
    StatusEnum statusPromoter,
    StatusEnum statusEmployee,
    StatusEnum statusTourGuide,
    List<AddressRequest> addresses,
    List<ContactRequest> contacts) {

  public record AddressRequest(
      String street, String number, String complement, String burgh, String postalCode, UUID cityId) {
  }

  public record ContactRequest(String name, String cellphone, String telephone, String email) {
  }
}
