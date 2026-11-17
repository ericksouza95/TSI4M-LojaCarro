package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;

import java.time.LocalDate;

// Filtros opcionais da consulta de agendamentos pelo administrador
public record AgendamentoFiltro(LocalDate data, Long veiculoId, StatusAgendamento status) {
}
