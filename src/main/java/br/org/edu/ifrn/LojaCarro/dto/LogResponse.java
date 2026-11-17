package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.LogSistema;

import java.time.LocalDateTime;

public record LogResponse(
        Long id,
        LocalDateTime dataHora,
        String acao,
        String entidade,
        String descricao,
        String usuario
) {
    public static LogResponse de(LogSistema l) {
        return new LogResponse(l.getId(), l.getDataHora(), l.getAcao(), l.getEntidade(), l.getDescricao(), l.getUsuario());
    }
}
