package br.org.edu.ifrn.LojaCarro.model;

public enum SituacaoVeiculo {
    DISPONIVEL,
    VENDIDO,
    // Exclusão lógica: o veículo sai do catálogo, mas continua no banco (histórico de agendamentos)
    INATIVO
}
