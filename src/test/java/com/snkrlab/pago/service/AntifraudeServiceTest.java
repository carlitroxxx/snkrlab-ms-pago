package com.snkrlab.pago.service;

import com.snkrlab.pago.model.*;
import com.snkrlab.pago.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AntifraudeServiceTest {
    final PagoRepository pagos = mock(PagoRepository.class);
    final ReferenciaAntifraudeRepository referencias = mock(ReferenciaAntifraudeRepository.class);
    final AlertaFraudeRepository alertas = mock(AlertaFraudeRepository.class);
    final Clock reloj = mock(Clock.class);
    final AntifraudeService service = new AntifraudeService(pagos, referencias, alertas, reloj, 10, 3);
    final List<Pago> historial = new ArrayList<>();
    AntifraudeServiceTest() {
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-09T12:00:00Z"));
        when(referencias.bloquear(anyString())).thenReturn(Optional.of(mock(ReferenciaAntifraude.class)));
        when(pagos.countByReferenciaPagoAndFechaGreaterThanAndFechaLessThanEqualAndIdNot(
                anyString(), any(), any(), anyLong())).thenAnswer(inv -> {
            String ref = inv.getArgument(0); LocalDateTime desde = inv.getArgument(1);
            LocalDateTime hasta = inv.getArgument(2); Long id = inv.getArgument(3);
            return historial.stream().filter(p -> ref.equals(p.getReferenciaPago())
                    && p.getFecha().isAfter(desde) && !p.getFecha().isAfter(hasta)
                    && !id.equals(p.getId())).count();
        });
    }
    Pago nuevo(long id, String ref, String usuario) {
        Pago p = new Pago(); p.setId(id); p.setIntentoId(UUID.randomUUID());
        p.setReferenciaPago(ref); p.setUsuarioId(usuario); p.setResultado(EstadoPago.RECHAZADO);
        return p;
    }
    @Test void tresPermitidosCuartoBloqueadoConUsuariosCompartidosYAlerta() {
        for (int i = 1; i <= 5; i++) {
            Pago p = nuevo(i, "SIM_APROBADA_01", i % 2 == 0 ? "usuario2" : "usuario1");
            assertEquals(i > 3, service.bloquearSiCorresponde(p));
            if (i > 3) assertEquals(EstadoPago.BLOQUEADO_FRAUDE, p.getResultado());
            else p.setResultado(i == 2 ? EstadoPago.RECHAZADO : EstadoPago.APROBADO);
            historial.add(p);
        }
        var captor = org.mockito.ArgumentCaptor.forClass(AlertaFraude.class);
        verify(alertas, times(2)).save(captor.capture());
        assertEquals(4L, captor.getAllValues().get(0).getCantidadIntentos());
        assertEquals(5L, captor.getValue().getCantidadIntentos());
        assertEquals(10, captor.getValue().getVentanaSegundos());
        assertEquals(3, captor.getValue().getMaxIntentos());
        assertEquals(historial.get(4).getIntentoId(), captor.getValue().getIntentoId());
        assertEquals("usuario1", captor.getValue().getUsuarioId());
        assertNotNull(captor.getValue().getMotivo());
    }
    @Test void referenciasIndependientesYVentanaExactaIncluyeHistoricosSinUuid() {
        for (int i = 1; i <= 3; i++) {
            Pago p = nuevo(i, "SIM_APROBADA_01", "usuario");
            p.setIntentoId(null); // Histórico: cuenta por fecha/referencia, sin filtrar UUID.
            p.setFecha(LocalDateTime.of(2026,11,9,12,0)); historial.add(p);
        }
        assertFalse(service.bloquearSiCorresponde(nuevo(4, "SIM_RECHAZADA_01", "otro")));
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-09T12:00:09.999Z"));
        assertTrue(service.bloquearSiCorresponde(nuevo(5, "SIM_APROBADA_01", "otro")));
        when(reloj.instant()).thenReturn(Instant.parse("2026-11-09T12:00:10Z"));
        assertFalse(service.bloquearSiCorresponde(nuevo(6, "SIM_APROBADA_01", "otro")));
    }
}
