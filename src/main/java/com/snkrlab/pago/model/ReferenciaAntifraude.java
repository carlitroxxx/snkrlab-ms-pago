package com.snkrlab.pago.model;
import jakarta.persistence.*;
@Entity
@Table(name = "referencias_antifraude")
public class ReferenciaAntifraude {
    @Id @Column(length = 32)
    private String referencia;
    protected ReferenciaAntifraude() {}
}
