package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.UsuarioAtualizacaoRequest;
import br.org.edu.ifrn.LojaCarro.dto.UsuarioCadastroRequest;
import br.org.edu.ifrn.LojaCarro.dto.UsuarioResponse;
import br.org.edu.ifrn.LojaCarro.exception.AcessoNegadoException;
import br.org.edu.ifrn.LojaCarro.exception.RecursoNaoEncontradoException;
import br.org.edu.ifrn.LojaCarro.exception.RegraNegocioException;
import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.repository.VeiculoRepository;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private static final String ENTIDADE = "Usuario";

    private final UsuarioRepository usuarioRepository;
    private final VeiculoRepository veiculoRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final UsuarioLogado usuarioLogado;

    public UsuarioService(UsuarioRepository usuarioRepository, VeiculoRepository veiculoRepository,
                          AgendamentoRepository agendamentoRepository, PasswordEncoder passwordEncoder,
                          LogService logService, UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.veiculoRepository = veiculoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
        this.usuarioLogado = usuarioLogado;
    }

    // Cadastro público: sempre cria um CLIENTE, nunca um administrador
    @Transactional
    public UsuarioResponse cadastrarCliente(UsuarioCadastroRequest dados) {
        String email = normalizarEmail(dados.email());
        validarEmailUnico(email, null);

        Usuario usuario = new Usuario(dados.nome().trim(), email, passwordEncoder.encode(dados.senha()), Perfil.CLIENTE);
        usuario.setTelefone(textoOuNulo(dados.telefone()));

        Usuario salvo = usuarioRepository.save(usuario);
        logService.registrar("CRIAR", ENTIDADE, "Cliente cadastrado: ID " + salvo.getId() + ", e-mail " + salvo.getEmail());
        return UsuarioResponse.de(salvo);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest dados) {
        verificarAcessoAosDados(id);
        Usuario usuario = buscarEntidade(id);
        String email = normalizarEmail(dados.email());
        validarEmailUnico(email, id);

        usuario.setNome(dados.nome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(textoOuNulo(dados.telefone()));
        if (dados.alterouSenha()) {
            usuario.setSenha(passwordEncoder.encode(dados.senha()));
        }

        Usuario salvo = usuarioRepository.save(usuario);
        logService.registrar("ATUALIZAR", ENTIDADE, "Usuário atualizado: ID " + id + ", e-mail " + salvo.getEmail()
                + (dados.alterouSenha() ? " (senha alterada)" : ""));
        return UsuarioResponse.de(salvo);
    }

    @Transactional
    public void excluir(Long id) {
        Usuario usuario = buscarEntidade(id);
        if (usuarioLogado.atual().map(u -> u.id().equals(id)).orElse(false)) {
            throw new RegraNegocioException("Não é possível excluir o usuário que está logado.");
        }
        if (veiculoRepository.existsByCadastradoPor(usuario)) {
            throw new RegraNegocioException("O usuário " + usuario.getEmail() + " possui veículos cadastrados e não pode ser excluído.");
        }
        if (agendamentoRepository.existsByCliente(usuario)) {
            throw new RegraNegocioException("O usuário " + usuario.getEmail() + " possui agendamentos e não pode ser excluído.");
        }
        usuarioRepository.delete(usuario);
        logService.registrar("EXCLUIR", ENTIDADE, "Usuário excluído: ID " + id + ", e-mail " + usuario.getEmail());
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        verificarAcessoAosDados(id);
        return UsuarioResponse.de(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    // Cliente só acessa os próprios dados; administrador acessa os de qualquer usuário
    private void verificarAcessoAosDados(Long id) {
        UsuarioAutenticado logado = usuarioLogado.obrigatorio();
        if (!logado.isAdmin() && !logado.id().equals(id)) {
            throw new AcessoNegadoException("Você só pode acessar os seus próprios dados.");
        }
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário com ID " + id + " não encontrado."));
    }

    // idAtual permite que o próprio usuário mantenha o seu e-mail ao ser atualizado
    private void validarEmailUnico(String email, Long idAtual) {
        usuarioRepository.findByEmail(email).ifPresent(outro -> {
            if (!outro.getId().equals(idAtual)) {
                throw new RegraNegocioException("Já existe um usuário com o e-mail " + email + ".");
            }
        });
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
