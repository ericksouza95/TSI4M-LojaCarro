package br.org.edu.ifrn.LojaCarro.security;

import br.org.edu.ifrn.LojaCarro.exception.AutenticacaoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Acesso ao usuário autenticado na requisição atual, para uso nos services
@Component
public class UsuarioLogado {

    public Optional<UsuarioAutenticado> atual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }

    public UsuarioAutenticado obrigatorio() {
        return atual().orElseThrow(() -> new AutenticacaoException("É necessário fazer login."));
    }

    public boolean isAdmin() {
        return atual().map(UsuarioAutenticado::isAdmin).orElse(false);
    }
}
