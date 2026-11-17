package br.org.edu.ifrn.LojaCarro.dto;

import java.time.Instant;

// Resposta do login: o frontend guarda o token e o envia no cabeçalho "Authorization: Bearer <token>"
public record LoginResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponse usuario
) {
}
