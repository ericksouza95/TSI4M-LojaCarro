package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

// Guarda na sessão HTTP qual usuário está logado
@Service
public class SessaoService {

    private static final String USUARIO_LOGADO = "usuarioLogadoId";

    @Autowired
    private UsuarioRepository usuarioRepository;

    public void entrar(Usuario usuario) {
        sessao(true).setAttribute(USUARIO_LOGADO, usuario.getId());
    }

    public void sair() {
        HttpSession sessao = sessao(false);
        if (sessao != null) {
            sessao.invalidate();
        }
    }

    public Optional<Usuario> usuarioLogado() {
        HttpSession sessao = sessao(false);
        if (sessao == null || sessao.getAttribute(USUARIO_LOGADO) == null) {
            return Optional.empty();
        }
        return usuarioRepository.findById((Long) sessao.getAttribute(USUARIO_LOGADO));
    }

    // Fora de uma requisição HTTP (ex.: inicialização do sistema) não existe sessão
    private HttpSession sessao(boolean criar) {
        ServletRequestAttributes atributos = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return atributos == null ? null : atributos.getRequest().getSession(criar);
    }
}
