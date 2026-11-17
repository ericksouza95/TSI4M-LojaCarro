package br.org.edu.ifrn.LojaCarro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// "Agora" é obtido deste relógio em vez de LocalDateTime.now(): nos testes ele pode ser substituído por um relógio fixo
@Configuration
public class RelogioConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemDefaultZone();
    }
}
