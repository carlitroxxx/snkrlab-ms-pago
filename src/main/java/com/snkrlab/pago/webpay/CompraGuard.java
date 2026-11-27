package com.snkrlab.pago.webpay;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Se invoca dentro de la transacción: serializa ambos proveedores por orden. */
@Component
public class CompraGuard {
    private final EntityManager em;
    public CompraGuard(EntityManager em) { this.em = em; }
    public void reservar(Long orden, String proveedor) {
        em.createNativeQuery("INSERT IGNORE INTO pago_compra_guard (orden_id, proveedor) VALUES (:id, :p)")
            .setParameter("id", orden).setParameter("p", proveedor).executeUpdate();
        Object actual = em.createNativeQuery("SELECT proveedor FROM pago_compra_guard WHERE orden_id=:id FOR UPDATE")
            .setParameter("id", orden).getSingleResult();
        if (!proveedor.equals(actual)) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "La compra ya tiene otro proveedor reservado.");
    }
}
