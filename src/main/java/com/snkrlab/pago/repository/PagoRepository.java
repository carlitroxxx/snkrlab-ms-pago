package com.snkrlab.pago.repository;

import com.snkrlab.pago.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByUsuarioIdOrderByFechaDesc(String usuarioId);
}
