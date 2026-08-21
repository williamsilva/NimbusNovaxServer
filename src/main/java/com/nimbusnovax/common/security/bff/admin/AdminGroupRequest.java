package com.nimbusnovax.common.security.bff.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mapeia pro GroupInput do NimbusAuth (mesmos limites de tamanho) - appKey não é campo aqui de
 *  propósito, é resolvido no NimbusAuth a partir do client autenticado (nimbusnovax-bff), nunca
 *  escolhido pelo caller (ver fix em GroupsController/GroupService no NimbusAuth). */
public record AdminGroupRequest(
    @NotBlank @Size(min = 3, max = 120) String name,
    @NotBlank @Size(min = 3, max = 1204) String description) {
}
