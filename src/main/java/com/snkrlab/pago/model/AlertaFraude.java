package com.snkrlab.pago.model;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity
@Table(name = "alertas_fraude")
public class AlertaFraude {
    public AlertaFraude() {}
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() { return id; }
    public void setId(Long valor) { this.id = valor; }
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable = false, length = 36, unique = true)
    private UUID intentoId;
    public UUID getIntentoId() { return intentoId; }
    public void setIntentoId(UUID valor) { this.intentoId = valor; }
    @Column(nullable = false, length = 32)
    private String referenciaPago;
    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String valor) { this.referenciaPago = valor; }
    @Column(nullable = false)
    private String usuarioId;
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String valor) { this.usuarioId = valor; }
    @Column(nullable = false)
    private LocalDateTime momento;
    public LocalDateTime getMomento() { return momento; }
    public void setMomento(LocalDateTime valor) { this.momento = valor; }
    @Column(nullable = false)
    private Long cantidadIntentos;
    public Long getCantidadIntentos() { return cantidadIntentos; }
    public void setCantidadIntentos(Long valor) { this.cantidadIntentos = valor; }
    @Column(nullable = false)
    private Integer ventanaSegundos;
    public Integer getVentanaSegundos() { return ventanaSegundos; }
    public void setVentanaSegundos(Integer valor) { this.ventanaSegundos = valor; }
    @Column(nullable = false)
    private Integer maxIntentos;
    public Integer getMaxIntentos() { return maxIntentos; }
    public void setMaxIntentos(Integer valor) { this.maxIntentos = valor; }
    @Column(nullable = false)
    private String motivo;
    public String getMotivo() { return motivo; }
    public void setMotivo(String valor) { this.motivo = valor; }
}
