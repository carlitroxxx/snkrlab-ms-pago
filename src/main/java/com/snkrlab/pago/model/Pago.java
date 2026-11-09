package com.snkrlab.pago.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pagos", uniqueConstraints = @UniqueConstraint(
        name = "uk_pagos_intento_id", columnNames = "intento_id"))
public class Pago {
    public Pago() {}

    // Nullable exclusivamente por compatibilidad con pagos históricos.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "intento_id", length = 36, updatable = false)
    private UUID intentoId;

    public UUID getIntentoId() { return intentoId; }
    public void setIntentoId(UUID valor) { this.intentoId = valor; }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ordenId;

    @Column(nullable = false)
    private String usuarioId;

    @Column(nullable = false)
    private Integer importe;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false, length = 32)
    private String referenciaPago;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoPago resultado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }

    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long valor) { this.ordenId = valor; }

    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String valor) { this.usuarioId = valor; }

    public Integer getImporte() { return importe; }
    public void setImporte(Integer valor) { this.importe = valor; }

    public String getMoneda() { return moneda; }
    public void setMoneda(String valor) { this.moneda = valor; }

    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String valor) { this.referenciaPago = valor; }

    public EstadoPago getResultado() { return resultado; }
    public void setResultado(EstadoPago valor) { this.resultado = valor; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime valor) { this.fecha = valor; }
}
