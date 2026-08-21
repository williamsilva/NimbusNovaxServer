package com.nimbusnovax.administracao.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** Item leve para popular os selects de itens do formulário de Voucher (ingressos/alimentação)
 *  sem carregar todos os campos de {@link ProductResponse} - só produtos ativos (ver
 *  ProductService.findOptions). */
public record ProductOptionResponse(UUID id, String name, BigDecimal amount) {
}
