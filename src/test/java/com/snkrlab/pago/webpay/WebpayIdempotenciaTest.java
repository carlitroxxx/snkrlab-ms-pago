package com.snkrlab.pago.webpay;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.springframework.transaction.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import com.snkrlab.pago.repository.PagoRepository;

class WebpayIdempotenciaTest {
    WebpayRepository repo=mock(WebpayRepository.class);
    WebpayGateway gateway=mock(WebpayGateway.class);
    OrdenConsultaClient ordenes=mock(OrdenConsultaClient.class);
    WebpayIntento w;
    WebpayService service;
    Jwt jwt=Jwt.withTokenValue("test").header("alg","RS256").claim("iss","https://cognito.example").subject("cliente").build();
    @BeforeEach void preparar() {
        var manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service=new WebpayService(repo,mock(PagoRepository.class),gateway,ordenes,mock(CompraGuard.class),manager,
            "http://localhost/retorno","https://cognito.example");
        w=new WebpayIntento(); w.ordenId=1L; w.checkoutId=UUID.randomUUID(); w.intentoId=UUID.randomUUID();
        w.usuarioId="cognito:cliente"; w.estado="PENDIENTE"; w.importe=1000; w.buyOrder="orden";
        w.sessionId="sesion"; w.token="token"; w.url="https://example.test";
        when(repo.findById(1L)).thenReturn(Optional.of(w));
        when(repo.bloquear(1L)).thenReturn(Optional.of(w));
        when(repo.findByToken("token")).thenReturn(Optional.of(w));
    }
    @Test void reintentoNoCreaOtraTransaccion() {
        var respuesta=service.crear(new WebpayService.Solicitud(w.intentoId,w.checkoutId,1L),jwt);
        assertEquals("token",respuesta.token()); verifyNoInteractions(gateway,ordenes);
        assertThrows(ResponseStatusException.class,()->service.crear(new WebpayService.Solicitud(UUID.randomUUID(),w.checkoutId,1L),jwt));
    }
    @Test void consultaNoExponeTokenYRechazaOtroPropietario() {
        assertNull(service.consultar(1L,jwt,false).token());
        w.usuarioId="otro";
        assertThrows(ResponseStatusException.class,()->service.consultar(1L,jwt,true));
        verifyNoInteractions(gateway);
    }
    @Test void commitInciertoNoSeRepite() throws Exception {
        when(gateway.confirmar("token")).thenThrow(new java.io.IOException());
        when(gateway.consultar("token")).thenReturn(new WebpayGateway.Resultado("orden","sesion",1000,"INITIALIZED",0));
        service.retornar(Map.of("token_ws","token"));
        service.retornar(Map.of("token_ws","token"));
        assertEquals("INCIERTO",w.estado);
        verify(gateway,times(1)).confirmar("token"); verify(gateway,times(1)).consultar("token");
    }
    @Test void anulacionYRetornoMixtoSoloConsultan() throws Exception {
        service.retornar(Map.of("TBK_TOKEN","token","token_ws","token"));
        verify(gateway,never()).confirmar(any()); verify(gateway).consultar("token");
    }
    @Test void tokenDesconocidoNoInvocaProveedor() {
        assertThrows(ResponseStatusException.class,()->service.retornar(Map.of("token_ws","ajeno")));
        verifyNoInteractions(gateway);
    }
    @Test void retornoTerminalNoRepiteConfirmacion() {
        w.estado="APROBADO"; w.pagoId=7L;
        service.retornar(Map.of("token_ws","token"));
        assertEquals(7L,service.consultar(1L,jwt,true).pagoId());
        verifyNoInteractions(gateway);
    }
    @Test void resultadoInciertoNoOfreceOtroFormulario() {
        w.estado="INCIERTO";
        assertNull(service.crear(new WebpayService.Solicitud(w.intentoId,w.checkoutId,1L),jwt).token());
        verifyNoInteractions(gateway);
    }
    @Test void anulacionCorrelacionadaPersisteYBloqueaCommitPosterior() throws Exception {
        when(gateway.consultar("token")).thenThrow(new java.io.IOException());
        service.retornar(Map.of("TBK_TOKEN","token","TBK_ORDEN_COMPRA","orden","TBK_ID_SESION","sesion"));
        assertEquals(Boolean.TRUE,w.anulacionRecibida);
        service.retornar(Map.of("token_ws","token"));
        verify(gateway,never()).confirmar(any());
        assertEquals("INCIERTO",w.estado);
    }
    @Test void anulacionAjenaNoConsultaYCommitPrevioNoPermiteMarcarAnulacion() throws Exception {
        assertThrows(ResponseStatusException.class,()->service.retornar(Map.of(
            "TBK_TOKEN","token","TBK_ORDEN_COMPRA","otra","TBK_ID_SESION","sesion")));
        verifyNoInteractions(gateway);
        w.commitIniciado=true;
        when(gateway.consultar("token")).thenReturn(new WebpayGateway.Resultado("orden","sesion",1000,"INITIALIZED",0));
        service.retornar(Map.of("TBK_TOKEN","token","TBK_ORDEN_COMPRA","orden","TBK_ID_SESION","sesion"));
        assertNull(w.anulacionRecibida);
        assertEquals("INCIERTO",w.estado);
    }}
