package com.snkrlab.pago.webpay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebpayServiceTest {
    private WebpayIntento intento() {
        var w=new WebpayIntento(); w.buyOrder="orden"; w.sessionId="sesion"; w.importe=1000; return w;
    }
    @Test void exigeMontoOrdenSesionYAutorizacion() {
        var w=intento();
        assertEquals("APROBADO",WebpayService.evaluar(w,new WebpayGateway.Resultado("orden","sesion",1000,"AUTHORIZED",0)));
        assertEquals("INCIERTO",WebpayService.evaluar(w,new WebpayGateway.Resultado("otra","sesion",1000,"AUTHORIZED",0)));
        assertEquals("INCIERTO",WebpayService.evaluar(w,new WebpayGateway.Resultado("orden","otra",1000,"AUTHORIZED",0)));
        assertEquals("INCIERTO",WebpayService.evaluar(w,new WebpayGateway.Resultado("orden","sesion",1,"AUTHORIZED",0)));
        assertEquals("INCIERTO",WebpayService.evaluar(w,new WebpayGateway.Resultado("orden","sesion",1000,"AUTHORIZED",-1)));
    }
    @Test void rechazoAnulacionYEstadoNoFinal() {
        var w=intento();
        assertEquals("RECHAZADO",WebpayService.evaluar(w,new WebpayGateway.Resultado("orden","sesion",1000,"FAILED",-1)));
        assertEquals("ANULADO",WebpayService.evaluar(w,new WebpayGateway.Resultado("orden","sesion",1000,"REVERSED",0)));
        assertEquals("INCIERTO",WebpayService.evaluar(w,new WebpayGateway.Resultado("orden","sesion",1000,"INITIALIZED",0)));
        assertEquals("INCIERTO",WebpayService.evaluar(w,null));
    }
    @Test void historicoEsSimulado() {
        assertEquals("SIMULADO",new com.snkrlab.pago.model.Pago().getProveedor());
    }
    @Test void initializedRequiereEvidenciaYAusenciaDeCommit() {
        var w=intento();
        var r=new WebpayGateway.Resultado("orden","sesion",1000,"INITIALIZED",0);
        assertEquals("INCIERTO",WebpayService.evaluar(w,r));
        w.anulacionRecibida=true;
        assertEquals("ANULADO",WebpayService.evaluar(w,r));
        w.commitIniciado=true;
        assertEquals("INCIERTO",WebpayService.evaluar(w,r));
        w.commitIniciado=false; w.pagoId=1L;
        assertEquals("INCIERTO",WebpayService.evaluar(w,r));
    }}
