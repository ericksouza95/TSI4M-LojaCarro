package br.org.edu.ifrn.LojaCarro.mensageria;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RabbitPublicadorEventos implements PublicadorEventos {

    private static final Logger logger = LoggerFactory.getLogger(RabbitPublicadorEventos.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitPublicadorEventos(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // Routing key "log.<entidade>" (ex.: log.veiculo), recebida pela fila do microserviço com o binding "log.#".
    // Se o RabbitMQ estiver fora do ar, a operação do usuário não é interrompida: a falha fica no log do servidor
    // (o evento também foi escrito no arquivo de log pelo LogService).
    @Override
    public void publicar(EventoAuditoria evento) {
        String routingKey = "log." + evento.entidade().toLowerCase(Locale.ROOT);
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_AUDITORIA, routingKey, evento);
        } catch (AmqpException e) {
            logger.error("Falha ao enviar evento de auditoria ao RabbitMQ: [{}] {}", evento.acao(), evento.descricao(), e);
        }
    }
}
