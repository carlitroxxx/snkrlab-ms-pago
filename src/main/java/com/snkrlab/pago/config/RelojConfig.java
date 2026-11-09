package com.snkrlab.pago.config;
import java.time.Clock;
import org.springframework.context.annotation.*;
@Configuration
public class RelojConfig {
    @Bean public Clock relojPago() { return Clock.systemUTC(); }
}
