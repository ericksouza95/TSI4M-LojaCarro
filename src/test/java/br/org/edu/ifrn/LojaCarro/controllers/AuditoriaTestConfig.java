package br.org.edu.ifrn.LojaCarro.controllers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class AuditoriaTestConfig {

    @Bean
    @Primary
    public PublicadorEmMemoria publicadorEmMemoria() {
        return new PublicadorEmMemoria();
    }
}
