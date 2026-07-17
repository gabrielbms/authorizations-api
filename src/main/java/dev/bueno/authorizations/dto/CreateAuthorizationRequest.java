package dev.bueno.authorizations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAuthorizationRequest(
        @NotBlank(message = "O campo userId é obrigatório.")
        String userId,

        @NotBlank(message = "O campo status é obrigatório.")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status deve ser ACTIVE ou INACTIVE")
        String status
) {}