package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;
import jakarta.validation.constraints.NotNull;

public record SituacaoVeiculoRequest(
        @NotNull(message = "A situação é obrigatória.")
        SituacaoVeiculo situacao
) {
}
