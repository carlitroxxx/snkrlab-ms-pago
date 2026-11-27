package com.snkrlab.pago.webpay;
import java.net.URI;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/pagos/webpay")
public class WebpayController {
    private final WebpayService service;
    private final URI frontend;
    public WebpayController(WebpayService service,@Value("${app.webpay.frontend-url}") String frontend) {
        this.service=service; this.frontend=URI.create(frontend);
        if (!this.frontend.isAbsolute() || !("https".equals(this.frontend.getScheme()) ||
            ("http".equals(this.frontend.getScheme()) && "localhost".equals(this.frontend.getHost()))))
            throw new IllegalArgumentException("URL frontend invalida");
    }
    @PostMapping("/transacciones")
    public ResponseEntity<WebpayService.Vista> crear(@RequestBody WebpayService.Solicitud solicitud,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.crear(solicitud,jwt));
    }
    @GetMapping("/ordenes/{id}")
    public WebpayService.Vista consultar(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) {
        return service.consultar(id,jwt,false);
    }
    @PostMapping("/ordenes/{id}/sincronizacion")
    public WebpayService.Vista sincronizar(@PathVariable Long id,@AuthenticationPrincipal Jwt jwt) {
        return service.consultar(id,jwt,true);
    }
    @RequestMapping(value="/retorno",method={RequestMethod.GET,RequestMethod.POST})
    public ResponseEntity<Void> retorno(@RequestParam Map<String,String> params) {
        service.retornar(params);
        return ResponseEntity.status(303).location(frontend).cacheControl(CacheControl.noStore())
            .header("Referrer-Policy","no-referrer").build();
    }
}
