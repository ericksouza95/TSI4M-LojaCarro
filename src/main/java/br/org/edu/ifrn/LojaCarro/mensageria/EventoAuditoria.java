package br.org.edu.ifrn.LojaCarro.mensageria;

import java.time.LocalDateTime;

// Mensagem (JSON) enviada ao microserviço de logs: horário, usuário, ação e detalhes da operação
public record EventoAuditoria(
        LocalDateTime dataHora,
        String acao,
        String entidade,
        String descricao,
        String usuario,
        String origem
) {
}
