package com.snkrlab.pago.service;

import com.snkrlab.pago.dto.*;
import com.snkrlab.pago.model.*;
import com.snkrlab.pago.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class PagoIdempotenciaTest {
    private final PagoRepository repository = mock(PagoRepository.class);
    private final SimuladorPagoService simulador = spy(new SimuladorPagoService());
    private final PagoService service = new PagoService(repository, simulador, mock(PlatformTransactionManager.class));
    private final UUID intento = UUID.randomUUID();
    private final PagoSolicitudDTO solicitud = new PagoSolicitudDTO(intento, 1L, 100, "SIM_APROBADA_01");

    private Pago existente() {
        Pago pago = new Pago();
        pago.setId(10L); pago.setIntentoId(intento); pago.setUsuarioId("usuario");
        pago.setOrdenId(1L); pago.setImporte(100); pago.setMoneda("CLP");
        pago.setReferenciaPago("SIM_APROBADA_01"); pago.setResultado(EstadoPago.APROBADO);
        pago.setFecha(java.time.LocalDateTime.of(2026, 11, 9, 12, 0));
        return pago;
    }

    @Test void repeticionDevuelveExactamenteElPersistidoSinSimular() {
        Pago pago = existente();
        for (EstadoPago estado : EstadoPago.values()) {
            pago.setResultado(estado);
            when(repository.findByIntentoId(intento)).thenReturn(Optional.of(pago));
            assertEquals(PagoRespuestaDTO.desde(pago), service.registrar("usuario", solicitud));
        }
        verifyNoInteractions(simulador);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void conflictosDeDatosYPropietarioNoExponenNiProcesanElPago() {
        when(repository.findByIntentoId(intento)).thenReturn(Optional.of(existente()));
        for (PagoSolicitudDTO cambio : List.of(new PagoSolicitudDTO(intento, 2L, 100, "SIM_APROBADA_01"),
                new PagoSolicitudDTO(intento, 1L, 101, "SIM_APROBADA_01"),
                new PagoSolicitudDTO(intento, 1L, 100, "SIM_RECHAZADA_01"))) {
            assertEquals(409, assertThrows(ResponseStatusException.class,
                    () -> service.registrar("usuario", cambio)).getStatusCode().value());
        }
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.registrar("otro", solicitud)).getStatusCode().value());
        verifyNoInteractions(simulador);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test void intentoObligatorio() {
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.registrar("usuario", new PagoSolicitudDTO(null, 1L, 100, "SIM_APROBADA_01")))
                .getStatusCode().value());
        verifyNoInteractions(repository, simulador);
    }

    @Test void colisionUniqueRecuperaGanadorSinSimular() {
        when(repository.findByIntentoId(intento)).thenReturn(Optional.empty(), Optional.of(existente()));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        assertEquals(10L, service.registrar("usuario", solicitud).id());
        verifyNoInteractions(simulador);
    }

    @Test void colisionConOtroPropietarioDevuelveConflicto() {
        when(repository.findByIntentoId(intento)).thenReturn(Optional.empty(), Optional.of(existente()));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.registrar("otro", solicitud)).getStatusCode().value());
        verifyNoInteractions(simulador);
    }

    @Test void esperaDeBloqueoAgotadaDevuelveConflictoControlado() {
        when(repository.findByIntentoId(intento)).thenThrow(new CannotAcquireLockException("timeout"));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.registrar("usuario", solicitud)).getStatusCode().value());
    }

    @Test void dosSolicitudesConcurrentesReservanUnaSolaVez() throws Exception {
        CyclicBarrier consultas = new CyclicBarrier(2);
        CountDownLatch finalizado = new CountDownLatch(1);
        AtomicReference<Pago> reservado = new AtomicReference<>();
        when(repository.findByIntentoId(intento)).thenAnswer(inv -> {
            if (reservado.get() == null) {
                consultas.await(5, TimeUnit.SECONDS);
                return Optional.empty();
            }
            return Optional.of(reservado.get());
        });
        when(repository.saveAndFlush(any(Pago.class))).thenAnswer(inv -> {
            Pago pago = inv.getArgument(0);
            if (reservado.compareAndSet(null, pago)) { pago.setId(10L); return pago; }
            if (reservado.get() == pago) { finalizado.countDown(); return pago; }
            assertTrue(finalizado.await(5, TimeUnit.SECONDS));
            throw new DataIntegrityViolationException("unique");
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<PagoRespuestaDTO> a = executor.submit(() -> service.registrar("usuario", solicitud));
            Future<PagoRespuestaDTO> b = executor.submit(() -> service.registrar("usuario", solicitud));
            assertEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            verify(simulador, times(1)).evaluar("SIM_APROBADA_01");
        } finally { executor.shutdownNow(); }
    }
}
