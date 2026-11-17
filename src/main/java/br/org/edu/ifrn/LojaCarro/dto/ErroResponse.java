package br.org.edu.ifrn.LojaCarro.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

// Formato padrão das respostas de erro da API.
// "campos" só aparece em erros de validação, com a mensagem de cada campo inválido.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        int status,
        String erro,
        Map<String, String> campos,
        String caminho,
        LocalDateTime dataHora
) {
}
