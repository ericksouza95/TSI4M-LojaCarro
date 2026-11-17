package br.org.edu.ifrn.LojaCarro.dto;

import java.time.LocalDateTime;

// Log de auditoria como devolvido pelo microserviço de logs
public record LogResponse(
        Long id,
        LocalDateTime dataHora,
        String acao,
        String entidade,
        String descricao,
        String usuario,
        String origem
) {
}
