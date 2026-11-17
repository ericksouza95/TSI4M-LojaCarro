package br.org.edu.ifrn.LojaCarro.controllers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

// Nos testes de integração, "agora" é sempre segunda-feira, 04/03/2030, às 09:00
@TestConfiguration
public class RelogioFixoTestConfig {

    public static final LocalDateTime AGORA = LocalDateTime.of(2030, 3, 4, 9, 0);

    @Bean
    @Primary
    public Clock relogioFixo() {
        ZoneId zona = ZoneId.systemDefault();
        return Clock.fixed(AGORA.atZone(zona).toInstant(), zona);
    }
}
