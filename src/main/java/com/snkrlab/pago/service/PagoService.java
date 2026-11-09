package com.snkrlab.pago.service;

import com.snkrlab.pago.dto.*;
import com.snkrlab.pago.model.Pago;
import com.snkrlab.pago.repository.PagoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class PagoService {
    private final PagoRepository repository;
    private final SimuladorPagoService simulador;

    public PagoService(PagoRepository repository, SimuladorPagoService simulador) {
        this.repository = repository;
        this.simulador = simulador;
    }

    @Transactional
    public PagoRespuestaDTO registrar(String usuarioId, PagoSolicitudDTO solicitud) {
        validarUsuario(usuarioId);
        if (solicitud == null || solicitud.ordenId() == null || solicitud.ordenId() <= 0
                || solicitud.importe() == null || solicitud.importe() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Orden e importe deben ser enteros positivos.");
        }
        Pago pago = new Pago();
        pago.setResultado(simulador.evaluar(solicitud.referenciaPago()));
        pago.setOrdenId(solicitud.ordenId());
        pago.setUsuarioId(usuarioId);
        pago.setImporte(solicitud.importe());
        pago.setMoneda("CLP");
        pago.setReferenciaPago(solicitud.referenciaPago());
        pago.setFecha(LocalDateTime.now(ZoneOffset.UTC));
        return PagoRespuestaDTO.desde(repository.save(pago));
    }

    @Transactional(readOnly = true)
    public List<PagoRespuestaDTO> consultarPorUsuario(String usuarioId) {
        validarUsuario(usuarioId);
        return repository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(PagoRespuestaDTO::desde).toList();
    }

    private void validarUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank() || usuarioId.length() > 255) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Identidad de usuario no válida.");
        }
    }
}
