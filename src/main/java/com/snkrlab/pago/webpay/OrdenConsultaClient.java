package com.snkrlab.pago.webpay;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Component
public class OrdenConsultaClient {
    private final RestClient http;
    public OrdenConsultaClient(@Value("${app.webpay.ordenes-url}") String url) {
        http = RestClient.builder().baseUrl(url).build();
    }
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Orden(Long id, UUID checkoutId, String usuarioId, Integer total, String moneda, String estado) {}
    public Orden consultar(Long id, Jwt jwt) {
        try {
            return http.get().uri("/api/v1/ordenes/{id}", id)
                .headers(h -> h.setBearerAuth(jwt.getTokenValue())).retrieve().body(Orden.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo verificar la orden.");
        }
    }
}
