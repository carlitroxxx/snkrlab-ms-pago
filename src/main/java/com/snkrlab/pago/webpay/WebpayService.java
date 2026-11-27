package com.snkrlab.pago.webpay;

import java.util.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.snkrlab.pago.model.*;
import com.snkrlab.pago.repository.PagoRepository;

@Service
public class WebpayService {
    private final WebpayRepository repo;
    private final PagoRepository pagos;
    private final WebpayGateway gateway;
    private final OrdenConsultaClient ordenes;
    private final CompraGuard guard;
    private final TransactionTemplate tx;
    private final String retorno;
    private final String issuer;
    public record Solicitud(UUID intentoId, UUID checkoutId, Long ordenId) {}
    public record Vista(Long ordenId, UUID checkoutId, UUID intentoId, Long pagoId, String estado,
                        Integer importe, String moneda, String token, String url) {}
    public WebpayService(WebpayRepository repo, PagoRepository pagos, WebpayGateway gateway,
            OrdenConsultaClient ordenes, CompraGuard guard, PlatformTransactionManager manager,
            @Value("${app.webpay.return-url}") String retorno,
            @Value("${app.seguridad.cognito-issuer}") String issuer) {
        this.repo=repo; this.pagos=pagos; this.gateway=gateway; this.ordenes=ordenes;
        this.guard=guard; this.tx=new TransactionTemplate(manager); this.retorno=retorno; this.issuer=issuer;
        var uri=java.net.URI.create(retorno);
        if (uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null ||
            !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && "localhost".equals(uri.getHost()))))
            throw new IllegalArgumentException("URL de retorno invalida");
    }
    public String usuario(Jwt jwt) {
        if (jwt == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        boolean cognito=issuer.equals(jwt.getClaimAsString("iss"));
        String id=cognito ? jwt.getSubject() : jwt.getClaimAsString("oid");
        if (id==null || id.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return cognito ? "cognito:"+id : id;
    }
    public Vista crear(Solicitud s, Jwt jwt) {
        String usuario=usuario(jwt);
        if (s==null || s.intentoId()==null || s.checkoutId()==null || s.ordenId()==null || s.ordenId()<=0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var existente=repo.findById(s.ordenId());
        if (existente.isPresent()) { validarReintento(existente.get(),s,usuario); return vista(existente.get(),true); }
        var orden=ordenes.consultar(s.ordenId(),jwt);
        if (orden==null || !s.ordenId().equals(orden.id()) || !s.checkoutId().equals(orden.checkoutId()) ||
            !usuario.equals(orden.usuarioId()) || !"PENDIENTE_PAGO".equals(orden.estado()) ||
            !"CLP".equals(orden.moneda()) || orden.total()==null || orden.total()<=0) throw conflicto();
        boolean[] nueva={false};
        var w=tx.execute(status -> {
            guard.reservar(s.ordenId(),"WEBPAY");
            var previa=repo.findById(s.ordenId());
            if (previa.isPresent()) { validarReintento(previa.get(),s,usuario); return previa.get(); }
            if (pagos.existsByOrdenId(s.ordenId())) throw conflicto();
            WebpayIntento n=new WebpayIntento();
            n.ordenId=s.ordenId(); n.checkoutId=s.checkoutId(); n.intentoId=s.intentoId();
            n.usuarioId=usuario; n.importe=orden.total(); n.sessionId=s.checkoutId().toString();
            n.buyOrder="S"+UUID.randomUUID().toString().replace("-", "").substring(0,25);
            n.estado="CREANDO"; nueva[0]=true;
            return repo.saveAndFlush(n);
        });
        if (!nueva[0]) return vista(w,true);
        try {
            var r=gateway.crear(w.buyOrder,w.sessionId,w.importe,retorno);
            if (r==null || r.token()==null || r.token().isBlank() || r.token().length()>128 ||
                r.url()==null || !r.url().startsWith("https://")) throw new IllegalStateException();
            w=tx.execute(status -> {
                var n=repo.bloquear(s.ordenId()).orElseThrow();
                n.token=r.token(); n.url=r.url(); n.estado="PENDIENTE"; return repo.saveAndFlush(n);
            });
        } catch (Exception e) { incierto(w.ordenId); w=repo.findById(w.ordenId).orElseThrow(); }
        return vista(w,true);
    }
    private void validarReintento(WebpayIntento w,Solicitud s,String usuario) {
        propietario(w,usuario);
        if (!s.checkoutId().equals(w.checkoutId) || !s.intentoId().equals(w.intentoId)) throw conflicto();
    }
    private void propietario(WebpayIntento w,String usuario) {
        if (!usuario.equals(w.usuarioId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    public Vista consultar(Long orden, Jwt jwt, boolean actualizar) {
        var w=repo.findById(orden).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        propietario(w,usuario(jwt));
        if (actualizar && w.token!=null && !terminal(w)) resolver(w.ordenId,false);
        return vista(repo.findById(orden).orElseThrow(),false);
    }
    public void retornar(Map<String,String> params) {
        String normal=params.get("token_ws"), abortado=params.get("TBK_TOKEN");
        Optional<WebpayIntento> encontrado;
        if (abortado!=null) encontrado=repo.findByToken(abortado);
        else if (normal!=null) encontrado=repo.findByToken(normal);
        else encontrado=repo.findByBuyOrderAndSessionId(params.get("TBK_ORDEN_COMPRA"),params.get("TBK_ID_SESION"));
        var w=encontrado.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,"Retorno desconocido."));
        if (abortado != null && normal == null) {
            if (!Objects.equals(w.buyOrder, params.get("TBK_ORDEN_COMPRA")) ||
                !Objects.equals(w.sessionId, params.get("TBK_ID_SESION")))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Anulacion no correlacionada.");
            tx.executeWithoutResult(status -> {
                var n=repo.bloquear(w.ordenId).orElseThrow();
                if (!terminal(n) && !n.commitIniciado && n.pagoId==null) n.anulacionRecibida=true;
            });
        }
        if (w.token!=null) resolver(w.ordenId,normal!=null && abortado==null);
    }
    private void resolver(Long orden, boolean confirmar) {
        boolean[] commit={false};
        var w=tx.execute(status -> {
            var n=repo.bloquear(orden).orElseThrow();
            if (!terminal(n) && confirmar && !n.commitIniciado && !Boolean.TRUE.equals(n.anulacionRecibida)) {
                n.commitIniciado=true; n.estado="CONFIRMANDO"; commit[0]=true;
            }
            return n;
        });
        if (terminal(w)) return;
        try {
            var r=commit[0] ? gateway.confirmar(w.token) : gateway.consultar(w.token);
            tx.executeWithoutResult(status -> {
                var n=repo.bloquear(orden).orElseThrow();
                if (terminal(n)) return;
                String estado=evaluar(n,r);
                n.estado=estado;
                if (terminal(n)) {
                    Pago p=new Pago(); p.setIntentoId(n.intentoId); p.setOrdenId(n.ordenId);
                    p.setUsuarioId(n.usuarioId); p.setImporte(n.importe); p.setMoneda("CLP");
                    p.setProveedor("WEBPAY"); p.setReferenciaPago(n.buyOrder);
                    p.setResultado(EstadoPago.valueOf(estado)); p.setFecha(LocalDateTime.now(ZoneOffset.UTC));
                    n.pagoId=pagos.saveAndFlush(p).getId();
                }
            });
        } catch (Exception e) { incierto(orden); }
    }
    static String evaluar(WebpayIntento n, WebpayGateway.Resultado r) {
        if (r==null || !n.buyOrder.equals(r.orden()) || !n.sessionId.equals(r.sesion()) ||
            Double.compare(n.importe.doubleValue(),r.importe())!=0) return "INCIERTO";
        if ("INITIALIZED".equals(r.estado()) && Boolean.TRUE.equals(n.anulacionRecibida)
            && !n.commitIniciado && n.pagoId==null) return "ANULADO";
        if ("AUTHORIZED".equals(r.estado()) && r.codigo()==0) return "APROBADO";
        if ("FAILED".equals(r.estado())) return "RECHAZADO";
        if ("REVERSED".equals(r.estado()) || "NULLIFIED".equals(r.estado())) return "ANULADO";
        return "INCIERTO";
    }
    private void incierto(Long id) {
        tx.executeWithoutResult(status -> { var w=repo.bloquear(id).orElseThrow(); if (!terminal(w)) w.estado="INCIERTO"; });
    }
    private boolean terminal(WebpayIntento w) { return Set.of("APROBADO","RECHAZADO","ANULADO").contains(w.estado); }
    private Vista vista(WebpayIntento w,boolean inicio) {
        boolean formulario=inicio && "PENDIENTE".equals(w.estado) && !w.commitIniciado;
        return new Vista(w.ordenId,w.checkoutId,w.intentoId,w.pagoId,w.estado,w.importe,"CLP",
            formulario?w.token:null,formulario?w.url:null);
    }
    private ResponseStatusException conflicto() { return new ResponseStatusException(HttpStatus.CONFLICT,"Compra incompatible o ya reservada."); }
}
