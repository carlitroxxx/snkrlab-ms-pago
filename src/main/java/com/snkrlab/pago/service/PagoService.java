package com.snkrlab.pago.service;

import com.snkrlab.pago.dto.*;
import com.snkrlab.pago.model.Pago;
import com.snkrlab.pago.model.EstadoPago;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Objects;
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
    private final com.snkrlab.pago.webpay.CompraGuard guard;
    private final PagoRepository repository;
    private final SimuladorPagoService simulador;
    private final AntifraudeService antifraude;

    private final TransactionTemplate transaccion;

    public PagoService(PagoRepository repository, SimuladorPagoService simulador,
            PlatformTransactionManager transactionManager, AntifraudeService antifraude,
            com.snkrlab.pago.webpay.CompraGuard guard) {
        this.guard = guard;
        this.repository = repository;
        this.antifraude = antifraude;
        this.simulador = simulador;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.transaccion.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public PagoRespuestaDTO registrar(String usuarioId, PagoSolicitudDTO solicitud) {
        validarUsuario(usuarioId);
        if (solicitud == null || solicitud.intentoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "intentoId es obligatorio.");
        }
        try {
            return transaccion.execute(status -> {
                var existente = repository.findByIntentoId(solicitud.intentoId());
                if (existente.isPresent()) return recuperar(existente.get(), usuarioId, solicitud);
                if (solicitud.ordenId() == null || solicitud.ordenId() <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
                guard.reservar(solicitud.ordenId(), "SIMULADO");
                return crear(usuarioId, solicitud);
            });
        } catch (DataIntegrityViolationException ex) {
            // La transacción fallida ya terminó: no consultar dentro de un rollback-only.
            return transaccion.execute(status -> repository.findByIntentoId(solicitud.intentoId())
                    .map(pago -> recuperar(pago, usuarioId, solicitud))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "No se pudo reservar el intento. Reintente con el mismo intentoId.")));
        } catch (TransientDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Intento concurrente en curso. Reintente con el mismo intentoId.");
        }
    }

    private PagoRespuestaDTO recuperar(Pago pago, String usuarioId, PagoSolicitudDTO solicitud) {
        if (!Objects.equals(pago.getUsuarioId(), usuarioId)
                || !Objects.equals(pago.getOrdenId(), solicitud.ordenId())
                || !Objects.equals(pago.getImporte(), solicitud.importe())
                || !Objects.equals(pago.getReferenciaPago(), solicitud.referenciaPago())
                || !Objects.equals(pago.getTarjetaId(), solicitud.tarjetaId())
                || !"CLP".equals(pago.getMoneda())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El intentoId ya está asociado a otros datos.");
        }
        return PagoRespuestaDTO.desde(pago);
    }

    private PagoRespuestaDTO crear(String usuarioId, PagoSolicitudDTO solicitud) {
        if (solicitud.ordenId() == null || solicitud.ordenId() <= 0
                || solicitud.importe() == null || solicitud.importe() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Orden e importe deben ser enteros positivos.");
        }
        if (!SimuladorPagoService.APROBADA.equals(solicitud.referenciaPago())
                && !SimuladorPagoService.RECHAZADA.equals(solicitud.referenciaPago())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referencia simulada no permitida.");
        }
        if (solicitud.tarjetaId() == null || !java.util.Set.of(
                "SIM_TARJETA_01", "SIM_TARJETA_02", "SIM_TARJETA_03").contains(solicitud.tarjetaId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "tarjetaId es obligatorio para pagos nuevos: use SIM_TARJETA_01, SIM_TARJETA_02 o SIM_TARJETA_03.");
        }
        Pago pago = new Pago();
        pago.setProveedor("SIMULADO");
        pago.setTarjetaId(solicitud.tarjetaId());
        pago.setIntentoId(solicitud.intentoId());
        // Valor provisional no visible fuera de esta transacción. Si falla, se revierte todo.
        pago.setResultado(EstadoPago.RECHAZADO);
        pago.setOrdenId(solicitud.ordenId());
        pago.setUsuarioId(usuarioId);
        pago.setImporte(solicitud.importe());
        pago.setMoneda("CLP");
        pago.setReferenciaPago(solicitud.referenciaPago());
        pago.setFecha(LocalDateTime.now(ZoneOffset.UTC));
        repository.saveAndFlush(pago); // Reserva UNIQUE antes de simular, incluso entre instancias.
        if (!antifraude.bloquearSiCorresponde(pago)) {
            pago.setResultado(simulador.evaluar(solicitud.referenciaPago()));
        }
        return PagoRespuestaDTO.desde(repository.saveAndFlush(pago));
    }

    @Transactional(readOnly = true)
    public List<PagoRespuestaDTO> consultarPorUsuario(String usuarioId) {
        validarUsuario(usuarioId);
        return repository.findByUsuarioIdOrderByFechaDesc(usuarioId).stream()
                .map(PagoRespuestaDTO::desde).toList();
    }

    @Transactional(readOnly = true)
    public PagoRespuestaDTO consultarPorId(Long id, String usuarioId) {
        validarUsuario(usuarioId);
        return repository.findByIdAndUsuarioId(id, usuarioId)
                .map(PagoRespuestaDTO::desde)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no encontrado."));
    }

    private void validarUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank() || usuarioId.length() > 255) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Identidad de usuario no válida.");
        }
    }
}
