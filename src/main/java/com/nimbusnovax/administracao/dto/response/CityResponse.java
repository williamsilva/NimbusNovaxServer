package com.nimbusnovax.administracao.dto.response;

import java.util.UUID;

public record CityResponse(UUID id, String name, UUID stateId) {
}
