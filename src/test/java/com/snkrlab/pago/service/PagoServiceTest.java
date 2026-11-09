package com.snkrlab.pago.service;

import com.snkrlab.pago.dto.PagoSolicitudDTO;
import com.snkrlab.pago.model.*;
import com.snkrlab.pago.repository.PagoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class PagoServiceTest {
    private final PagoRepository repository = mock(PagoRepository.class);
    private final PagoService service = new PagoService(repository, new SimuladorPagoService());

    @Test void persisteCadaResultadoConIdentidadMonedaYFecha() {
        when(repository.save(any(Pago.class))).thenAnswer(invocation -> {
            Pago pago = invocation.getArgument(0);
            assertNull(pago.getId());
            pago.setId(1L);
            return pago;
        });
        for (String referencia : List.of("SIM_APROBADA_01", "SIM_RECHAZADA_01")) {
            var respuesta = service.registrar("cognito:usuario", new PagoSolicitudDTO(12L, 15000, referencia));
            assertEquals(1L, respuesta.id());
            assertEquals(12L, respuesta.ordenId());
            assertEquals("cognito:usuario", respuesta.usuarioId());
            assertEquals(15000, respuesta.importe());
            assertEquals("CLP", respuesta.moneda());
            assertEquals(referencia, respuesta.referenciaPago());
            assertNotNull(respuesta.fecha());
            assertEquals(referencia.equals("SIM_APROBADA_01") ? EstadoPago.APROBADO : EstadoPago.RECHAZADO,
                    respuesta.resultado());
        }
        verify(repository, times(2)).save(any(Pago.class));
    }

    @Test void noPersisteSolicitudesInvalidas() {
        for (PagoSolicitudDTO solicitud : List.of(
                new PagoSolicitudDTO(null, 100, "SIM_APROBADA_01"),
                new PagoSolicitudDTO(0L, 100, "SIM_APROBADA_01"),
                new PagoSolicitudDTO(1L, null, "SIM_APROBADA_01"),
                new PagoSolicitudDTO(1L, 0, "SIM_APROBADA_01"),
                new PagoSolicitudDTO(1L, -1, "SIM_APROBADA_01"),
                new PagoSolicitudDTO(1L, 100, "desconocida"))) {
            assertThrows(ResponseStatusException.class, () -> service.registrar("usuario", solicitud));
        }
        assertThrows(ResponseStatusException.class, () -> service.registrar("usuario", null));
        assertThrows(ResponseStatusException.class, () -> service.registrar("", new PagoSolicitudDTO(1L, 1, "SIM_APROBADA_01")));
        verifyNoInteractions(repository);
    }

    @Test void consultaSoloElUsuarioIndicado() {
        when(repository.findByUsuarioIdOrderByFechaDesc("usuario")).thenReturn(List.of());
        assertTrue(service.consultarPorUsuario("usuario").isEmpty());
        verify(repository).findByUsuarioIdOrderByFechaDesc("usuario");
        assertThrows(ResponseStatusException.class, () -> service.consultarPorUsuario(null));
    }
}
