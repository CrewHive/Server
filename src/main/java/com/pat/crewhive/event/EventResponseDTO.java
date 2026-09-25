package com.pat.crewhive.event;

import jakarta.validation.constraints.NotNull;

/**
 * Risposta dell'utente a un invito a un evento.
 */
public record EventResponseDTO(

        @NotNull(message = "The answer cannot be null")
        Boolean accepted
) {
}
