package br.org.edu.ifrn.LojaCarro.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

// Um horário de 30 minutos na grade do dia, usado pelo frontend para o cliente escolher
public record HorarioDisponivelResponse(
        @JsonFormat(pattern = "HH:mm") LocalTime inicio,
        @JsonFormat(pattern = "HH:mm") LocalTime fim,
        boolean disponivel
) {
}
