package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.LoginRequest;
import br.org.edu.ifrn.LojaCarro.dto.LoginResponse;
import br.org.edu.ifrn.LojaCarro.dto.UsuarioResponse;
import br.org.edu.ifrn.LojaCarro.exception.AutenticacaoException;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.security.JwtService;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final String ENTIDADE = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UsuarioLogado usuarioLogado;
    private final LogService logService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       UsuarioLogado usuarioLogado, LogService logService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.usuarioLogado = usuarioLogado;
        this.logService = logService;
    }

    // Confere e-mail e senha (BCrypt) e devolve um token JWT.
    // A mesma mensagem é usada para e-mail inexistente e senha errada, para não revelar quais e-mails existem.
    public LoginResponse login(LoginRequest dados) {
        String email = dados.email().trim().toLowerCase(Locale.ROOT);
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(dados.senha(), u.getSenha()));
        if (encontrado.isEmpty()) {
            logService.registrar("FALHA_LOGIN", ENTIDADE, "Tentativa de login inválida para o e-mail " + email, "anônimo");
            throw new AutenticacaoException("E-mail ou senha inválidos.");
        }
        Usuario usuario = encontrado.get();
        JwtService.TokenGerado token = jwtService.gerarToken(usuario);
        logService.registrar("LOGIN", ENTIDADE, "Login realizado: " + usuario.getEmail(),
                UsuarioAutenticado.de(usuario).descricao());
        return new LoginResponse(token.token(), "Bearer", token.expiraEm(), UsuarioResponse.de(usuario));
    }

    // Com JWT o servidor não guarda sessão: o frontend descarta o token. Aqui só registramos a saída.
    public void logout() {
        logService.registrar("LOGOUT", ENTIDADE, "Logout realizado: " + usuarioLogado.obrigatorio().email());
    }

    public UsuarioResponse usuarioAtual() {
        Long id = usuarioLogado.obrigatorio().id();
        return usuarioRepository.findById(id)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new AutenticacaoException("Usuário não encontrado."));
    }
}
