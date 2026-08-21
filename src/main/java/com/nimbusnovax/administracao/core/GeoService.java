package com.nimbusnovax.administracao.core;

import com.nimbusnovax.administracao.dto.response.CityResponse;
import com.nimbusnovax.administracao.dto.response.StateResponse;
import com.nimbusnovax.administracao.repository.CityRepository;
import com.nimbusnovax.administracao.repository.StateRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados de referência (estado/cidade) pro seletor de Endereço da tela de Agentes - leitura aberta
 *  a qualquer usuário autenticado, sem authority própria (mesmo critério de Supplier.options()). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeoService {

  private final StateRepository stateRepository;
  private final CityRepository cityRepository;

  public List<StateResponse> states() {
    return stateRepository.findAllByOrderByNameAsc().stream()
        .map(s -> new StateResponse(s.getId(), s.getName(), s.getUf()))
        .toList();
  }

  public List<CityResponse> citiesByState(UUID stateId) {
    return cityRepository.findAllByStateIdOrderByNameAsc(stateId).stream()
        .map(c -> new CityResponse(c.getId(), c.getName(), c.getState().getId()))
        .toList();
  }
}
