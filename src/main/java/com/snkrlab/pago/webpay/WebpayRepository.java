package com.snkrlab.pago.webpay;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface WebpayRepository extends JpaRepository<WebpayIntento, Long> {
    Optional<WebpayIntento> findByToken(String token);
    Optional<WebpayIntento> findByBuyOrderAndSessionId(String buyOrder, String sessionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WebpayIntento w where w.ordenId = :id")
    Optional<WebpayIntento> bloquear(Long id);
}
