package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.UsuarioException;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final String ENTIDADE = "Usuario";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogService logService;

    @Autowired
    private SessaoService sessaoService;

    @Autowired
    private CarroRepository carroRepository;

    public Usuario login(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .filter(u -> senha != null && u.getSenha().equals(criptografarSenha(senha)))
                .orElseThrow(() -> new UsuarioException("E-mail ou senha inválidos."));
        sessaoService.entrar(usuario);
        logService.registrar("LOGIN", ENTIDADE, "Login realizado: " + usuario.getEmail());
        return usuario;
    }

    public void logout() {
        sessaoService.usuarioLogado().ifPresent(u ->
                logService.registrar("LOGOUT", ENTIDADE, "Logout realizado: " + u.getEmail()));
        sessaoService.sair();
    }

    public Optional<Usuario> usuarioLogado() {
        return sessaoService.usuarioLogado();
    }

    public Usuario save(Usuario u) {
        validarNome(u.getNome());
        validarEmail(u.getEmail(), null);
        validarSenha(u.getSenha());
        u.setId(null);
        u.setSenha(criptografarSenha(u.getSenha()));
        u.setDataCadastro(LocalDateTime.now());
        Usuario salvo = usuarioRepository.save(u);
        logService.registrar("CRIAR", ENTIDADE, "Usuário criado: ID " + salvo.getId() + ", e-mail " + salvo.getEmail());
        return salvo;
    }

    public Usuario update(Usuario u) {
        if (u.getId() == null) {
            throw new UsuarioException("O ID do usuário para atualização não pode ser nulo.");
        }
        Usuario existente = usuarioRepository.findById(u.getId())
                .orElseThrow(() -> new UsuarioException("Usuário com ID " + u.getId() + " não encontrado para atualização."));
        validarNome(u.getNome());
        validarEmail(u.getEmail(), u.getId());

        existente.setNome(u.getNome());
        existente.setEmail(u.getEmail());
        // Senha em branco na atualização mantém a senha atual
        if (u.getSenha() != null && !u.getSenha().isBlank()) {
            validarSenha(u.getSenha());
            existente.setSenha(criptografarSenha(u.getSenha()));
        }
        Usuario atualizado = usuarioRepository.save(existente);
        logService.registrar("ATUALIZAR", ENTIDADE, "Usuário atualizado: ID " + atualizado.getId() + ", e-mail " + atualizado.getEmail());
        return atualizado;
    }

    public void deleteById(Long id) {
        validarId(id);
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioException("Usuário com ID " + id + " não encontrado para exclusão."));
        if (sessaoService.usuarioLogado().map(logado -> logado.getId().equals(id)).orElse(false)) {
            throw new UsuarioException("Não é possível excluir o usuário que está logado.");
        }
        if (carroRepository.existsByCadastradoPor(usuario)) {
            throw new UsuarioException("O usuário " + usuario.getEmail() + " possui carros cadastrados e não pode ser excluído.");
        }
        usuarioRepository.delete(usuario);
        logService.registrar("EXCLUIR", ENTIDADE, "Usuário excluído: ID " + id + ", e-mail " + usuario.getEmail());
    }

    public Optional<Usuario> findById(Long id) {
        validarId(id);
        logService.registrar("CONSULTAR", ENTIDADE, "Consulta do usuário ID " + id);
        return usuarioRepository.findById(id);
    }

    public List<Usuario> findAll() {
        logService.registrar("CONSULTAR", ENTIDADE, "Listagem de todos os usuários");
        return usuarioRepository.findAll();
    }

    private void validarId(Long id) {
        if (id == null || id <= 0) {
            throw new UsuarioException("O ID do usuário deve ser positivo. ID fornecido: " + id);
        }
    }

    private void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new UsuarioException("O nome do usuário não pode estar vazio.");
        }
    }

    // idAtual permite que o próprio usuário mantenha o seu e-mail ao ser atualizado
    private void validarEmail(String email, Long idAtual) {
        if (email == null || !email.contains("@")) {
            throw new UsuarioException("E-mail inválido: " + email);
        }
        usuarioRepository.findByEmail(email).ifPresent(outro -> {
            if (!outro.getId().equals(idAtual)) {
                throw new UsuarioException("Já existe um usuário com o e-mail " + email);
            }
        });
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < 6) {
            throw new UsuarioException("A senha deve ter pelo menos 6 caracteres.");
        }
    }

    private String criptografarSenha(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(senha.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
