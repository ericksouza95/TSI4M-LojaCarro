package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.UsuarioCadastroRequest;
import br.org.edu.ifrn.LojaCarro.dto.UsuarioResponse;
import br.org.edu.ifrn.LojaCarro.exception.AcessoNegadoException;
import br.org.edu.ifrn.LojaCarro.exception.RegraNegocioException;
import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.repository.VeiculoRepository;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LogService logService;

    @Mock
    private UsuarioLogado usuarioLogado;

    @InjectMocks
    private UsuarioService usuarioService;

    private static Usuario usuario(Long id, String email, Perfil perfil) {
        Usuario u = new Usuario("Fulano", email, "hash-guardado", perfil);
        u.setId(id);
        return u;
    }

    private static UsuarioAutenticado autenticado(Long id, Perfil perfil) {
        return new UsuarioAutenticado(id, "Fulano", "fulano@email.com", perfil);
    }

    @Test
    void cadastroPublicoCriaClienteComSenhaCriptografada() {
        when(usuarioRepository.findByEmail("ana@email.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("segredo123")).thenReturn("hash-bcrypt");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse resposta = usuarioService.cadastrarCliente(
                new UsuarioCadastroRequest("Ana", " Ana@Email.com ", "segredo123", ""));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getSenha()).isEqualTo("hash-bcrypt");
        assertThat(salvo.getValue().getPerfil()).isEqualTo(Perfil.CLIENTE);
        assertThat(salvo.getValue().getTelefone()).isNull();
        assertThat(resposta.email()).isEqualTo("ana@email.com");
    }

    @Test
    void naoCadastraEmailJaUtilizado() {
        when(usuarioRepository.findByEmail("ana@email.com"))
                .thenReturn(Optional.of(usuario(1L, "ana@email.com", Perfil.CLIENTE)));

        assertThatThrownBy(() -> usuarioService.cadastrarCliente(
                new UsuarioCadastroRequest("Outra Ana", "ana@email.com", "segredo123", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ana@email.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void clienteNaoConsultaDadosDeOutroCliente() {
        when(usuarioLogado.obrigatorio()).thenReturn(autenticado(1L, Perfil.CLIENTE));

        assertThatThrownBy(() -> usuarioService.buscarPorId(2L))
                .isInstanceOf(AcessoNegadoException.class);
        verify(usuarioRepository, never()).findById(any());
    }

    @Test
    void clienteConsultaOsPropriosDados() {
        when(usuarioLogado.obrigatorio()).thenReturn(autenticado(1L, Perfil.CLIENTE));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ana@email.com", Perfil.CLIENTE)));

        assertThat(usuarioService.buscarPorId(1L).email()).isEqualTo("ana@email.com");
    }

    @Test
    void administradorConsultaDadosDeQualquerUsuario() {
        when(usuarioLogado.obrigatorio()).thenReturn(autenticado(9L, Perfil.ADMIN));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario(2L, "bruno@email.com", Perfil.CLIENTE)));

        assertThat(usuarioService.buscarPorId(2L).email()).isEqualTo("bruno@email.com");
    }

    @Test
    void naoExcluiUsuarioComAgendamentos() {
        Usuario cliente = usuario(3L, "cli@email.com", Perfil.CLIENTE);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(cliente));
        when(usuarioLogado.atual()).thenReturn(Optional.of(autenticado(1L, Perfil.ADMIN)));
        when(veiculoRepository.existsByCadastradoPor(cliente)).thenReturn(false);
        when(agendamentoRepository.existsByCliente(cliente)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.excluir(3L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("agendamentos");
        verify(usuarioRepository, never()).delete(any());
    }
}
