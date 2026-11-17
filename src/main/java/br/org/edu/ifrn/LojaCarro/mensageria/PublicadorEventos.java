package br.org.edu.ifrn.LojaCarro.mensageria;

// Envia os eventos de auditoria para fora da aplicação (em produção, para o RabbitMQ)
public interface PublicadorEventos {

    void publicar(EventoAuditoria evento);
}
