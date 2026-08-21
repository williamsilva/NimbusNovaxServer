package com.nimbusnovax.administracao.dto.response;

import com.nimbusnovax.administracao.model.enums.CivilStateEnum;
import com.nimbusnovax.administracao.model.enums.StatusEnum;
import com.nimbusnovax.administracao.model.enums.TypeAgentEnum;
import com.nimbusnovax.administracao.model.enums.TypePersonEnum;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AgentResponse(
    UUID id,
    String code,
    String name,
    String socialReason,
    String document,
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
    List<AddressResponse> addresses,
    List<ContactResponse> contacts,
    Instant createdAt,
    Instant updatedAt) {

  public record AddressResponse(
      UUID id, String street, String number, String complement, String burgh, String postalCode,
      UUID cityId, String cityName, String stateUf) {
  }

  public record ContactResponse(UUID id, String name, String cellphone, String telephone, String email) {
  }
}
