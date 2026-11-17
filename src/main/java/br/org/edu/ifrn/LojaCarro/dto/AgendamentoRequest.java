package br.org.edu.ifrn.LojaCarro.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

// O cliente informa o veículo, a data (AAAA-MM-DD) e o horário de início (HH:mm). A duração é sempre de 30 minutos.
public record AgendamentoRequest(
        @NotNull(message = "O veículo é obrigatório.")
        Long veiculoId,

        @NotNull(message = "A data é obrigatória.")
        LocalDate data,

        @NotNull(message = "O horário é obrigatório.")
        LocalTime horario
) {
}
