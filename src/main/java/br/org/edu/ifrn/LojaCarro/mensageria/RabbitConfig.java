package br.org.edu.ifrn.LojaCarro.mensageria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// A API só conhece a exchange. A fila (e a fila de descarte) são declaradas pelo microserviço de logs, que as consome.
@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_AUDITORIA = "lojacarro.auditoria";

    @Bean
    public TopicExchange exchangeAuditoria() {
        return new TopicExchange(EXCHANGE_AUDITORIA, true, false);
    }

    // Mensagens em JSON, com datas no formato ISO (mesmo ObjectMapper da API)
    @Bean
    public MessageConverter conversorJson(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
