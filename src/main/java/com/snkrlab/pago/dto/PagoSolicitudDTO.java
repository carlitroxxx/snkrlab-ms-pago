package com.snkrlab.pago.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/** tarjetaId identifica exclusivamente una tarjeta ficticia del simulador. */
public record PagoSolicitudDTO(UUID intentoId, Long ordenId, Integer importe, String referenciaPago, String tarjetaId) {
    public PagoSolicitudDTO(UUID intentoId, Long ordenId, Integer importe, String referenciaPago) {
        this(intentoId, ordenId, importe, referenciaPago, null);
    }
    @JsonAnySetter
    public void rechazarCampoDesconocido(String nombre, Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }
}
