package com.snkrlab.pago.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import com.snkrlab.pago.model.EstadoPago;

class SimuladorPagoServiceTest {
    private final SimuladorPagoService service = new SimuladorPagoService();
    @Test void apruebaReferenciaDefinida() {
        assertEquals(EstadoPago.APROBADO, service.evaluar("SIM_APROBADA_01"));
    }
    @Test void rechazaReferenciaDefinida() {
        assertEquals(EstadoPago.RECHAZADO, service.evaluar("SIM_RECHAZADA_01"));
    }
    @Test void noAdmiteReferenciasDesconocidasNiDatosDeTarjeta() {
        for (String valor : new String[]{null, "", "4111111111111111", "123", "12/30", "sim_aprobada_01"}) {
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> service.evaluar(valor)).getStatusCode().value());
        }
    }
}
