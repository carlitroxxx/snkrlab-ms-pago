package com.snkrlab.pago.controller;

import com.snkrlab.pago.dto.*;
import com.snkrlab.pago.service.PagoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Endpoint provisional de simulación. No confirma ni cobra órdenes reales. */
@RestController
@Profile("local")
@RequestMapping("/api/v1/pagos")
public class PagoController {
    private final PagoService service;
    private final String cognitoIssuer;

    public PagoController(PagoService service,
            @Value("${app.seguridad.cognito-issuer}") String cognitoIssuer) {
        this.service = service;
        this.cognitoIssuer = cognitoIssuer;
    }

    @PostMapping
    public ResponseEntity<PagoRespuestaDTO> registrar(@AuthenticationPrincipal Jwt jwt,
            @RequestBody PagoSolicitudDTO solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.registrar(usuarioIdDesde(jwt), solicitud));
    }

    @GetMapping("/{id}")
    public PagoRespuestaDTO consultar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.consultarPorId(id, usuarioIdDesde(jwt));
    }

    private String usuarioIdDesde(Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        boolean esCognito = cognitoIssuer.equals(jwt.getClaimAsString("iss"));
        String id = esCognito ? jwt.getSubject() : jwt.getClaimAsString("oid");
        if (id == null || id.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "El token no contiene el identificador del usuario.");
        }
        return esCognito ? "cognito:" + id : id;
    }
}
