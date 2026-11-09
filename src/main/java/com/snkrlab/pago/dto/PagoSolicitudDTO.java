package com.snkrlab.pago.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/** Contrato provisional: orden e importe suministrados solo para pruebas locales. */
public record PagoSolicitudDTO(UUID intentoId, Long ordenId, Integer importe, String referenciaPago) {
    @JsonAnySetter
    public void rechazarCampoDesconocido(String nombre, Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }
}
