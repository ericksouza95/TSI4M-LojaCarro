package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.model.Veiculo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AgendamentoResponse(
        Long id,
        Long veiculoId,
        String veiculo,
        String veiculoFotoUrl,
        Long clienteId,
        String clienteNome,
        String clienteEmail,
        String clienteTelefone,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horarioInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horarioFim,
        StatusAgendamento status,
        boolean cancelavel,
        LocalDateTime dataCriacao,
        LocalDateTime dataCancelamento
) {
    public static AgendamentoResponse de(Agendamento a, LocalDateTime agora) {
        Veiculo v = a.getVeiculo();
        Usuario c = a.getCliente();
        return new AgendamentoResponse(
                a.getId(),
                v.getId(),
                v.getMarca() + " " + v.getModelo() + " " + v.getAnoFabricacao() + "/" + v.getAnoModelo(),
                VeiculoResponse.de(v).fotoUrl(),
                c.getId(),
                c.getNome(),
                c.getEmail(),
                c.getTelefone(),
                a.getDataHoraInicio().toLocalDate(),
                a.getDataHoraInicio().toLocalTime(),
                a.getDataHoraFim().toLocalTime(),
                a.getStatus(),
                a.podeSerCancelado(agora),
                a.getDataCriacao(),
                a.getDataCancelamento());
    }
}
