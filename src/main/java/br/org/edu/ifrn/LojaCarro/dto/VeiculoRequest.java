package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.Combustivel;
import br.org.edu.ifrn.LojaCarro.model.Transmissao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

// Dados enviados para cadastrar ou atualizar um veículo
public record VeiculoRequest(
        @NotBlank(message = "A marca é obrigatória.")
        @Size(max = 60, message = "A marca deve ter no máximo 60 caracteres.")
        String marca,

        @NotBlank(message = "O modelo é obrigatório.")
        @Size(max = 80, message = "O modelo deve ter no máximo 80 caracteres.")
        String modelo,

        @NotNull(message = "O ano de fabricação é obrigatório.")
        @Min(value = 1900, message = "O ano de fabricação deve ser a partir de 1900.")
        Integer anoFabricacao,

        @NotNull(message = "O ano do modelo é obrigatório.")
        @Min(value = 1900, message = "O ano do modelo deve ser a partir de 1900.")
        Integer anoModelo,

        @NotBlank(message = "A cor é obrigatória.")
        @Size(max = 40, message = "A cor deve ter no máximo 40 caracteres.")
        String cor,

        @NotNull(message = "A quilometragem é obrigatória.")
        @PositiveOrZero(message = "A quilometragem não pode ser negativa.")
        Integer quilometragem,

        @NotNull(message = "O tipo de combustível é obrigatório.")
        Combustivel combustivel,

        @NotNull(message = "O tipo de transmissão é obrigatório.")
        Transmissao transmissao,

        @NotNull(message = "O preço é obrigatório.")
        @Positive(message = "O preço deve ser maior que zero.")
        @Digits(integer = 10, fraction = 2, message = "O preço deve ter no máximo 10 dígitos inteiros e 2 decimais.")
        BigDecimal preco,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres.")
        String descricao
) {
}
