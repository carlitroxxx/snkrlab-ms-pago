package com.snkrlab.pago.webpay;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import org.springframework.stereotype.Component;

@Component
public class WebpayGateway {
    // Credenciales PUBLICAS oficiales de integración. No existe modo producción.
    private final WebpayPlus.Transaction sdk = WebpayPlus.Transaction.buildForIntegration(
        "597055555532", "579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C");
    public record Creada(String token, String url) {}
    public record Resultado(String orden, String sesion, double importe, String estado, int codigo) {}
    public Creada crear(String orden, String sesion, int importe, String retorno) throws Exception {
        var r = sdk.create(orden, sesion, importe, retorno);
        return new Creada(r.getToken(), r.getUrl());
    }
    public Resultado confirmar(String token) throws Exception {
        var r = sdk.commit(token);
        return new Resultado(r.getBuyOrder(), r.getSessionId(), r.getAmount(), r.getStatus(), r.getResponseCode());
    }
    public Resultado consultar(String token) throws Exception {
        var r = sdk.status(token);
        return new Resultado(r.getBuyOrder(), r.getSessionId(), r.getAmount(), r.getStatus(), r.getResponseCode());
    }
}
