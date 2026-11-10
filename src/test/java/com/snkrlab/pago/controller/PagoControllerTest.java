package com.snkrlab.pago.controller;

import com.snkrlab.pago.model.Pago;
import com.snkrlab.pago.model.EstadoPago;
import com.snkrlab.pago.repository.PagoRepository;
import com.snkrlab.pago.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PagoControllerTest {
    final PagoRepository repository = mock(PagoRepository.class);
    final SimuladorPagoService simulador = mock(SimuladorPagoService.class);
    final AntifraudeService antifraude = mock(AntifraudeService.class);
    Jwt jwt;
    MockMvc mvc;

    @BeforeEach void configurar() {
        jwt = Jwt.withTokenValue("test").header("alg", "none")
                .claim("iss", "https://azure.test").claim("oid", "usuario").build();
        PagoService service = new PagoService(repository, simulador,
                mock(PlatformTransactionManager.class), antifraude);
        mvc = MockMvcBuilders.standaloneSetup(new PagoController(service, "https://cognito.test"))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType() == Jwt.class;
                    }
                    @Override public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container,
                            NativeWebRequest request, WebDataBinderFactory binderFactory) {
                        return jwt;
                    }
                }).build();
    }

    void prepararPago() {
        Pago pago = new Pago();
        pago.setId(1L); pago.setUsuarioId("usuario"); pago.setOrdenId(12L);
        pago.setImporte(15000); pago.setMoneda("CLP"); pago.setResultado(EstadoPago.APROBADO);
        when(repository.findByIdAndUsuarioId(1L, "usuario")).thenReturn(Optional.of(pago));
    }

    @Test void propioDevuelve200ConDatosPersistidos() throws Exception {
        prepararPago();
        mvc.perform(get("/api/v1/pagos/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.usuarioId").value("usuario"))
                .andExpect(jsonPath("$.ordenId").value(12))
                .andExpect(jsonPath("$.importe").value(15000))
                .andExpect(jsonPath("$.moneda").value("CLP"))
                .andExpect(jsonPath("$.resultado").value("APROBADO"));
        verify(repository).findByIdAndUsuarioId(1L, "usuario");
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(simulador, antifraude);
    }

    @Test void inexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/v1/pagos/99")).andExpect(status().isNotFound());
        verify(repository).findByIdAndUsuarioId(99L, "usuario");
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(simulador, antifraude);
    }

    @Test void ajenoDevuelve404AunqueExistaParaSuPropietario() throws Exception {
        prepararPago();
        jwt = Jwt.withTokenValue("test").header("alg", "none")
                .claim("iss", "https://azure.test").claim("oid", "otro").build();
        mvc.perform(get("/api/v1/pagos/1")).andExpect(status().isNotFound());
        verify(repository).findByIdAndUsuarioId(1L, "otro");
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(simulador, antifraude);
    }

    @Test void cognitoUtilizaPrefijoYSubject() throws Exception {
        jwt = Jwt.withTokenValue("test").header("alg", "none")
                .claim("iss", "https://cognito.test").subject("usuario").build();
        mvc.perform(get("/api/v1/pagos/1")).andExpect(status().isNotFound());
        verify(repository).findByIdAndUsuarioId(1L, "cognito:usuario");
    }

    @Test void sinIdentidadNoConsultaPagos() throws Exception {
        jwt = Jwt.withTokenValue("test").header("alg", "none")
                .claim("iss", "https://azure.test").build();
        mvc.perform(get("/api/v1/pagos/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository, simulador, antifraude);
    }
}
