package com.nimbusnovax.administracao.dto.response;

import java.util.UUID;

/** Item leve para popular selects (cliente/promotor/guia turístico do formulário de Voucher) sem
 *  carregar a árvore completa de endereços/contatos de {@link AgentResponse}. */
public record AgentOptionResponse(UUID id, String name, String document, boolean inactive) {
}
