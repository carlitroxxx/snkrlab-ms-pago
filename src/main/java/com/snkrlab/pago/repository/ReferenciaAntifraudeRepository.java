package com.snkrlab.pago.repository;
import com.snkrlab.pago.model.ReferenciaAntifraude;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface ReferenciaAntifraudeRepository extends JpaRepository<ReferenciaAntifraude, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReferenciaAntifraude r where r.referencia = :referencia")
    Optional<ReferenciaAntifraude> bloquear(@Param("referencia") String referencia);
}
