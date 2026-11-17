package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.mensageria.EventoAuditoria;
import br.org.edu.ifrn.LojaCarro.mensageria.PublicadorEventos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Nos testes, os eventos de auditoria ficam numa lista em vez de irem para o RabbitMQ
public class PublicadorEmMemoria implements PublicadorEventos {

    private final List<EventoAuditoria> publicados = new CopyOnWriteArrayList<>();

    @Override
    public void publicar(EventoAuditoria evento) {
        publicados.add(evento);
    }

    public List<EventoAuditoria> eventos() {
        return List.copyOf(publicados);
    }

    public EventoAuditoria ultimo() {
        return publicados.get(publicados.size() - 1);
    }

    public void limpar() {
        publicados.clear();
    }
}
