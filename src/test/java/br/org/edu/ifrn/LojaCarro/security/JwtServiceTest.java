package br.org.edu.ifrn.LojaCarro.security;

import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";

    private final JwtService jwtService = new JwtService(SEGREDO, 60);

    private static Usuario usuario(long id) {
        Usuario u = new Usuario("Ana", "ana@email.com", "hash", Perfil.CLIENTE);
        u.setId(id);
        return u;
    }

    @Test
    void tokenGeradoEhValidadoERetornaOIdDoUsuario() {
        JwtService.TokenGerado gerado = jwtService.gerarToken(usuario(42L));

        assertThat(jwtService.extrairUsuarioId(gerado.token())).contains(42L);
        assertThat(gerado.expiraEm()).isAfter(Instant.now().plus(59, ChronoUnit.MINUTES));
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRejeitado() {
        JwtService outroServidor = new JwtService("outro-segredo-qualquer-com-32-caracteres-ou-mais", 60);
        String tokenDeOutroServidor = outroServidor.gerarToken(usuario(1L)).token();

        assertThat(jwtService.extrairUsuarioId(tokenDeOutroServidor)).isEmpty();
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        String expirado = jwtService.gerarToken(usuario(1L), Instant.now().minus(61, ChronoUnit.MINUTES)).token();

        assertThat(jwtService.extrairUsuarioId(expirado)).isEmpty();
    }

    @Test
    void textoQueNaoEhJwtEhRejeitado() {
        assertThat(jwtService.extrairUsuarioId("abc.def.ghi")).isEmpty();
        assertThat(jwtService.extrairUsuarioId("")).isEmpty();
    }

    @Test
    void segredoCurtoImpedeAInicializacao() {
        assertThatThrownBy(() -> new JwtService("curto", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }
}
