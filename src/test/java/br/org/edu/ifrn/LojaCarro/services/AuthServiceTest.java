package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.LoginRequest;
import br.org.edu.ifrn.LojaCarro.dto.LoginResponse;
import br.org.edu.ifrn.LojaCarro.exception.AutenticacaoException;
import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.security.JwtService;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioLogado usuarioLogado;

    @Mock
    private LogService logService;

    @InjectMocks
    private AuthService authService;

    private static Usuario ana() {
        Usuario u = new Usuario("Ana", "ana@email.com", "hash-guardado", Perfil.CLIENTE);
        u.setId(1L);
        return u;
    }

    @Test
    void loginComSenhaCorretaDevolveTokenJwt() {
        Usuario ana = ana();
        Instant expira = Instant.now().plusSeconds(3600);
        when(usuarioRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("segredo123", "hash-guardado")).thenReturn(true);
        when(jwtService.gerarToken(ana)).thenReturn(new JwtService.TokenGerado("token-jwt", expira));

        LoginResponse resposta = authService.login(new LoginRequest(" Ana@Email.com ", "segredo123"));

        assertThat(resposta.token()).isEqualTo("token-jwt");
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.expiraEm()).isEqualTo(expira);
        assertThat(resposta.usuario().perfil()).isEqualTo(Perfil.CLIENTE);
        verify(logService).registrar(eq("LOGIN"), eq("Usuario"), anyString(), eq("Ana (ana@email.com)"));
    }

    @Test
    void loginComSenhaErradaNaoGeraTokenERegistraFalha() {
        when(usuarioRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(ana()));
        when(passwordEncoder.matches("errada", "hash-guardado")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@email.com", "errada")))
                .isInstanceOf(AutenticacaoException.class)
                .hasMessage("E-mail ou senha inválidos.");
        verify(jwtService, never()).gerarToken(any());
        verify(logService).registrar(eq("FALHA_LOGIN"), eq("Usuario"), anyString(), eq("anônimo"));
    }

    @Test
    void loginComEmailInexistenteTemAMesmaMensagemDeErro() {
        when(usuarioRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@email.com", "qualquer")))
                .isInstanceOf(AutenticacaoException.class)
                .hasMessage("E-mail ou senha inválidos.");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).gerarToken(any());
    }
}
