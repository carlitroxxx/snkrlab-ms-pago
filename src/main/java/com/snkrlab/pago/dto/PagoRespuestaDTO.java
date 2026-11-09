package com.snkrlab.pago.dto;

import com.snkrlab.pago.model.EstadoPago;
import com.snkrlab.pago.model.Pago;
import java.time.LocalDateTime;

public record PagoRespuestaDTO(Long id, Long ordenId, String usuarioId,
        Integer importe, String moneda, String referenciaPago,
        EstadoPago resultado, LocalDateTime fecha) {
    public static PagoRespuestaDTO desde(Pago pago) {
        return new PagoRespuestaDTO(pago.getId(), pago.getOrdenId(), pago.getUsuarioId(),
                pago.getImporte(), pago.getMoneda(), pago.getReferenciaPago(),
                pago.getResultado(), pago.getFecha());
    }
}
