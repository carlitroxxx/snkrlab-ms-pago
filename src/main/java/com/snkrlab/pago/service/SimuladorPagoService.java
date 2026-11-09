package com.snkrlab.pago.service;

import com.snkrlab.pago.model.EstadoPago;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SimuladorPagoService {
    public static final String APROBADA = "SIM_APROBADA_01";
    public static final String RECHAZADA = "SIM_RECHAZADA_01";

    public EstadoPago evaluar(String referencia) {
        if (APROBADA.equals(referencia)) return EstadoPago.APROBADO;
        if (RECHAZADA.equals(referencia)) return EstadoPago.RECHAZADO;
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Referencia simulada no permitida.");
    }
}
