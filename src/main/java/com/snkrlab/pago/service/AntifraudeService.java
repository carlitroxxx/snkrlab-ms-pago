package com.snkrlab.pago.service;

import com.snkrlab.pago.model.*;
import com.snkrlab.pago.repository.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AntifraudeService {
    private final PagoRepository pagos;
    private final ReferenciaAntifraudeRepository referencias;
    private final AlertaFraudeRepository alertas;
    private final Clock reloj;
    private final int ventana;
    private final int maximo;

    public AntifraudeService(PagoRepository pagos, ReferenciaAntifraudeRepository referencias,
            AlertaFraudeRepository alertas, Clock reloj,
            @Value("${app.antifraude.ventana-segundos:10}") int ventana,
            @Value("${app.antifraude.max-intentos:3}") int maximo) {
        if (ventana <= 0 || maximo <= 0) throw new IllegalArgumentException("Umbrales antifraude inválidos");
        this.pagos = pagos; this.referencias = referencias; this.alertas = alertas;
        this.reloj = reloj; this.ventana = ventana; this.maximo = maximo;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean bloquearSiCorresponde(Pago pago) {
        referencias.bloquear(pago.getReferenciaPago()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Falta configurar la referencia antifraude en MySQL."));
        // Tomar la hora tras adquirir el bloqueo. Se mantiene hasta commit/rollback.
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        pago.setFecha(ahora);
        long cantidad = pagos.countByReferenciaPagoAndFechaGreaterThanAndFechaLessThanEqualAndIdNot(
                pago.getReferenciaPago(), ahora.minusSeconds(ventana), ahora, pago.getId()) + 1;
        if (cantidad <= maximo) return false;
        pago.setResultado(EstadoPago.BLOQUEADO_FRAUDE);
        AlertaFraude alerta = new AlertaFraude();
        alerta.setIntentoId(pago.getIntentoId());
        alerta.setReferenciaPago(pago.getReferenciaPago());
        alerta.setUsuarioId(pago.getUsuarioId());
        alerta.setMomento(ahora);
        alerta.setCantidadIntentos(cantidad);
        alerta.setVentanaSegundos(ventana);
        alerta.setMaxIntentos(maximo);
        alerta.setMotivo("Más de " + maximo + " usos de la referencia en " + ventana + " segundos");
        alertas.save(alerta);
        return true;
    }
}
