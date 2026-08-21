package com.nimbusnovax.common.security.bff.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import java.util.UUID;

/**
 * Mapeia pro UserInput do NimbusAuth (userName = e-mail = login, sem campo de senha - modelo é de
 * convite: NimbusAuth cria com status PENDING_PASSWORD e dispara e-mail; ver
 * UserController.resendInvite). groupIds aqui são só os grupos do NimbusNovax selecionados no
 * formulário - AdminUserService funde com os grupos de outros apps que o usuário já tenha antes
 * de enviar pro NimbusAuth (PUT faz replace total da lista de grupos).
 */
public record AdminUserRequest(
    @NotBlank @Email String userName,
    @NotBlank String name,
    @NotBlank String document,
    @NotEmpty Set<UUID> groupIds) {
}
