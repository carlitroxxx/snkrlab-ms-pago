package com.snkrlab.pago.service;

import com.snkrlab.pago.dto.*;
import com.snkrlab.pago.model.EstadoPago;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in: usa MySQL real y conserva los registros de prueba. */
@SpringBootTest(properties = {"app.antifraude.ventana-segundos=10", "app.antifraude.max-intentos=3"})
@Import(AntifraudeMySqlTest.RelojPrueba.class)
@EnabledIfEnvironmentVariable(named = "PAGO_MYSQL_TEST", matches = "true")
class AntifraudeMySqlTest {
    @TestConfiguration static class RelojPrueba {
        @Bean @Primary Clock relojFijo() { return Clock.fixed(Instant.now(), ZoneOffset.UTC); }
    }
    @Autowired PagoService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired Clock reloj;

    @Test void cuatroIntentosConcurrentesConservanUmbralYReenviosNoDuplicanAlertas() throws Exception {
        LocalDateTime ahora = LocalDateTime.ofInstant(reloj.instant(), ZoneOffset.UTC);
        long previos = jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE referencia_pago = ? AND fecha > ? AND fecha <= ?",
                Long.class, "SIM_APROBADA_01", ahora.minusSeconds(10), ahora);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch inicio = new CountDownLatch(1);
        List<PagoSolicitudDTO> solicitudes = new ArrayList<>();
        List<Future<PagoRespuestaDTO>> futuros = new ArrayList<>();
        try {
            for (int i=0; i<4; i++) {
                var solicitud = new PagoSolicitudDTO(UUID.randomUUID(), 900001L, 100, "SIM_APROBADA_01");
                solicitudes.add(solicitud);
                String usuario = "prueba-antifraude-" + i % 2;
                futuros.add(executor.submit(() -> { inicio.await(); return service.registrar(usuario, solicitud); }));
            }
            inicio.countDown();
            int aprobados = 0;
            for (int i=0; i<4; i++) {
                var resultado = futuros.get(i).get(30, TimeUnit.SECONDS);
                if (resultado.resultado() == EstadoPago.APROBADO) aprobados++;
                else assertEquals(EstadoPago.BLOQUEADO_FRAUDE, resultado.resultado());
                assertEquals(resultado, service.registrar("prueba-antifraude-" + i % 2, solicitudes.get(i)));
                assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM pagos WHERE intento_id = ?",
                        Long.class, resultado.intentoId().toString()));
                assertEquals(resultado.resultado() == EstadoPago.BLOQUEADO_FRAUDE ? 1L : 0L,
                        jdbc.queryForObject("SELECT COUNT(*) FROM alertas_fraude WHERE intento_id = ?",
                                Long.class, resultado.intentoId().toString()));
            }
            assertEquals(Math.max(0, 3 - previos), (long) aprobados);
        } finally { executor.shutdownNow(); }
    }
}
