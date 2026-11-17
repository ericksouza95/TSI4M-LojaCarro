package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;

import java.math.BigDecimal;
import java.util.List;

// Filtros opcionais do catálogo. "busca" procura na marca ou no modelo.
public record VeiculoFiltro(
        String busca,
        BigDecimal precoMin,
        BigDecimal precoMax,
        Integer anoMin,
        Integer anoMax,
        List<SituacaoVeiculo> situacoes
) {
    public boolean temBusca() {
        return busca != null && !busca.isBlank();
    }

    public VeiculoFiltro comSituacoes(List<SituacaoVeiculo> novasSituacoes) {
        return new VeiculoFiltro(busca, precoMin, precoMax, anoMin, anoMax, novasSituacoes);
    }
}
