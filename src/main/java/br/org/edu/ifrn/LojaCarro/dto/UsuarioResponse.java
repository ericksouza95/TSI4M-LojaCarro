package br.org.edu.ifrn.LojaCarro.dto;

import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;

import java.time.LocalDateTime;

// Dados públicos do usuário: a senha nunca sai da API
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        Perfil perfil,
        LocalDateTime dataCadastro
) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPerfil(), u.getDataCadastro());
    }
}
