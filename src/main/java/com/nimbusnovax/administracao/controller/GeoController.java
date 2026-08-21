package com.nimbusnovax.administracao.controller;

import com.nimbusnovax.administracao.core.GeoService;
import com.nimbusnovax.administracao.dto.response.CityResponse;
import com.nimbusnovax.administracao.dto.response.StateResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Estados/cidades - dado de referência pro seletor de Endereço da tela de Agentes. */
@RestController
@RequiredArgsConstructor
public class GeoController {

  private final GeoService service;

  @GetMapping("/bff/v1/states")
  public List<StateResponse> states() {
    return service.states();
  }

  @GetMapping("/bff/v1/cities")
  public List<CityResponse> cities(@RequestParam UUID stateId) {
    return service.citiesByState(stateId);
  }
}
