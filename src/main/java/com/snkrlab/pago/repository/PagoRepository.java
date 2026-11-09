package com.snkrlab.pago.repository;

import com.snkrlab.pago.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByIntentoId(UUID intentoId);

    List<Pago> findByUsuarioIdOrderByFechaDesc(String usuarioId);
}
