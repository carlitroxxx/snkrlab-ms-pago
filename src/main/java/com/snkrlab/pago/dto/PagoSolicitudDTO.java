package com.snkrlab.pago.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/** Contrato provisional: orden e importe suministrados solo para pruebas locales. */
public record PagoSolicitudDTO(Long ordenId, Integer importe, String referenciaPago) {
    @JsonAnySetter
    public void rechazarCampoDesconocido(String nombre, Object valor) {
        throw new IllegalArgumentException("La solicitud contiene campos no permitidos.");
    }
}
