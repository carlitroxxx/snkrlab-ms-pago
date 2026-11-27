package com.snkrlab.pago.config;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

class ErrorDispatchTest {
    static ConfigurableApplicationContext context;
    static String base;
    @Configuration(proxyBeanMethods=false)
    @EnableAutoConfiguration(excludeName="org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
    @Import({TestSecurity.class, Probe.class, com.snkrlab.pago.webpay.WebpayController.class})
    static class App {
        @Bean com.snkrlab.pago.webpay.WebpayService service() {
            return new com.snkrlab.pago.webpay.WebpayService(
                mock(com.snkrlab.pago.webpay.WebpayRepository.class),
                mock(com.snkrlab.pago.repository.PagoRepository.class),
                mock(com.snkrlab.pago.webpay.WebpayGateway.class),
                mock(com.snkrlab.pago.webpay.OrdenConsultaClient.class),
                mock(com.snkrlab.pago.webpay.CompraGuard.class),
                mock(org.springframework.transaction.PlatformTransactionManager.class),
                "http://localhost/retorno","https://cognito.example");
        }
    }
    @Configuration
    static class TestSecurity extends SecurityConfig {
        @Override @Bean public JwtDecoder jwtDecoder() {
            return token -> {
                if (!List.of("cliente","administrador").contains(token)) throw new BadJwtException("Token de prueba invalido");
                return Jwt.withTokenValue(token).header("alg","RS256").subject("test")
                    .claim("iss","https://cognito.example")
                    .claim("cognito:groups",List.of(token.equals("cliente")?"Cliente":"Administrador")).build();
            };
        }
    }
    @RestController static class Probe {
        @GetMapping("/prueba-protegida") String protegida() {return "ok";}
    }
    @BeforeAll static void start() {
        context=SpringApplication.run(App.class,
            "--spring.config.location=optional:classpath:/security-isolated.properties",
            "--server.address=127.0.0.1","--server.port=0",
            "--spring.main.banner-mode=off",
            "--spring.security.oauth2.resourceserver.jwt.tenant-id=test",
            "--app.seguridad.azure-client-id=test","--app.seguridad.cognito-client-id=test",
            "--app.seguridad.cognito-issuer=https://cognito.example",
            "--app.pago.url=http://127.0.0.1:1",
            "--app.webpay.frontend-url=http://localhost:5173/pago/resultado");
        base="http://127.0.0.1:"+context.getEnvironment().getProperty("local.server.port");
    }
    @AfterAll static void stop() {if(context!=null) context.close();}
    int request(String method,String path,String token) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(base+path)).method(method,HttpRequest.BodyPublishers.noBody());
        if(token!=null) builder.header("Authorization","Bearer "+token);
        return HttpClient.newHttpClient().send(builder.build(),HttpResponse.BodyHandlers.discarding()).statusCode();
    }
    @Test void retornoGetSinParametros() throws Exception {assertEquals(400,request("GET","/api/v1/pagos/webpay/retorno",null));}
    @Test void retornoPostSinParametros() throws Exception {assertEquals(400,request("POST","/api/v1/pagos/webpay/retorno",null));}
    @Test void errorDirectoNoEsPublico() throws Exception {assertEquals(401,request("GET","/error",null));}
    @Test void rutasProtegidasExigenJwtYRol() throws Exception {
        assertEquals(401,request("GET","/prueba-protegida",null));
        assertEquals(401,request("GET","/prueba-protegida","invalido"));
        assertEquals(403,request("GET","/prueba-protegida","administrador"));
        assertEquals(200,request("GET","/prueba-protegida","cliente"));
        assertEquals(401,request("POST","/api/v1/pagos/webpay/transacciones",null));
    }
}