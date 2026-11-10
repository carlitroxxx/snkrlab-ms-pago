package com.snkrlab.pago.repository;

import com.snkrlab.pago.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    long countByReferenciaPagoAndFechaGreaterThanAndFechaLessThanEqualAndIdNot(
            String referencia, java.time.LocalDateTime desde, java.time.LocalDateTime hasta, Long id);

    Optional<Pago> findByIntentoId(UUID intentoId);
    Optional<Pago> findByIdAndUsuarioId(Long id, String usuarioId);

    List<Pago> findByUsuarioIdOrderByFechaDesc(String usuarioId);
}
