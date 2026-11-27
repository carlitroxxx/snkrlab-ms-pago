package com.snkrlab.pago.webpay;

import jakarta.persistence.*;
import java.util.UUID;

/** Datos internos: nunca serializar esta entidad en una respuesta o log. */
@Entity
@Table(name = "webpay_intentos")
public class WebpayIntento {
    @Id public Long ordenId;
    @Column(nullable=false, unique=true) public UUID checkoutId;
    @Column(nullable=false, unique=true) public UUID intentoId;
    @Column(nullable=false) public String usuarioId;
    @Column(nullable=false) public Integer importe;
    @Column(nullable=false, unique=true, length=26) public String buyOrder;
    @Column(nullable=false, length=61) public String sessionId;
    @Column(unique=true, length=128) public String token;
    @Column(length=2048) public String url;
    @Column(nullable=false, length=24) public String estado;
    public Long pagoId;
    public boolean commitIniciado;
    public Boolean anulacionRecibida;
}
