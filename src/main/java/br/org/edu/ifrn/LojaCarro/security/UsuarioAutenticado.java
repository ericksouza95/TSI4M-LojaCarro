package br.org.edu.ifrn.LojaCarro.security;

import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

// Usuário identificado pelo token JWT da requisição atual (fica no SecurityContext)
public record UsuarioAutenticado(Long id, String nome, String email, Perfil perfil) {

    public static UsuarioAutenticado de(Usuario u) {
        return new UsuarioAutenticado(u.getId(), u.getNome(), u.getEmail(), u.getPerfil());
    }

    public boolean isAdmin() {
        return perfil == Perfil.ADMIN;
    }

    // Perfil ADMIN vira a autoridade ROLE_ADMIN, usada em hasRole("ADMIN")
    public List<GrantedAuthority> autoridades() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    // Identificação usada nos logs de auditoria
    public String descricao() {
        return nome + " (" + email + ")";
    }
}
