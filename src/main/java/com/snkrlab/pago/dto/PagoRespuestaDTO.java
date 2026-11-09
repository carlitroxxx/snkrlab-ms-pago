package com.snkrlab.pago.dto;

import com.snkrlab.pago.model.EstadoPago;
import com.snkrlab.pago.model.Pago;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoRespuestaDTO(Long id, UUID intentoId, Long ordenId, String usuarioId,
        Integer importe, String moneda, String referenciaPago,
        EstadoPago resultado, LocalDateTime fecha) {
    public static PagoRespuestaDTO desde(Pago pago) {
        return new PagoRespuestaDTO(pago.getId(), pago.getIntentoId(), pago.getOrdenId(), pago.getUsuarioId(),
                pago.getImporte(), pago.getMoneda(), pago.getReferenciaPago(),
                pago.getResultado(), pago.getFecha());
    }
}
