package com.snkrlab.pago.controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import com.snkrlab.pago.webpay.WebpayService;
import com.snkrlab.pago.service.PagoService;
import com.snkrlab.pago.dto.PagoRespuestaDTO;
@RestController
public class PagoConsultaController {
    private final PagoService pagos;
    private final WebpayService webpay;
    public PagoConsultaController(PagoService pagos,WebpayService webpay) {this.pagos=pagos;this.webpay=webpay;}
    @GetMapping("/api/v1/pagos/{id}")
    public PagoRespuestaDTO consultar(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) {
        return pagos.consultarPorId(id,webpay.usuario(jwt));
    }
}
