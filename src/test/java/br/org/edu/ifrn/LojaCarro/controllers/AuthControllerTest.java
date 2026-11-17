package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Autenticação JWT: geração do token, validação e rotas protegidas
class AuthControllerTest extends IntegracaoTestBase {

    @Autowired
    private JwtService jwtService;

    @Test
    void loginGeraTokenJwtComOsDadosDoUsuario() throws Exception {
        criarCliente("Ana", "ana@email.com");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ANA@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                // Formato JWT: cabeçalho.payload.assinatura
                .andExpect(jsonPath("$.token").value(matchesPattern("^[\\w-]+\\.[\\w-]+\\.[\\w-]+$")))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(jsonPath("$.usuario.email").value("ana@email.com"))
                .andExpect(jsonPath("$.usuario.perfil").value("CLIENTE"))
                .andExpect(jsonPath("$.usuario.senha").doesNotExist());

        assertThat(logRepository.findAllByOrderByIdDesc().get(0).getUsuario()).isEqualTo("Ana (ana@email.com)");
    }

    @Test
    void tokenDoLoginDaAcessoAoUsuarioAtual() throws Exception {
        String token = tokenDeCliente("Ana", "ana@email.com");

        mockMvc.perform(get("/api/auth/me").with(token(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@email.com"));
    }

    @Test
    void loginComSenhaErradaRetorna401EFicaNoLog() throws Exception {
        criarCliente("Joao", "joao@email.com");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joao@email.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."))
                .andExpect(jsonPath("$.token").doesNotExist());

        assertThat(logRepository.findAllByOrderByIdDesc().get(0).getAcao()).isEqualTo("FALHA_LOGIN");
    }

    @Test
    void endpointProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("É necessário fazer login."));
        mockMvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/logs")).andExpect(status().isUnauthorized());

        assertThat(logRepository.findAllByOrderByIdDesc())
                .hasSize(3)
                .extracting("acao").containsOnly("ACESSO_NEGADO");
    }

    @Test
    void tokenAdulteradoEhRejeitado() throws Exception {
        String token = tokenDeCliente("Ana", "ana@email.com");
        // Troca o primeiro caractere da assinatura (terceira parte do JWT)
        int inicioAssinatura = token.lastIndexOf('.') + 1;
        char original = token.charAt(inicioAssinatura);
        String adulterado = token.substring(0, inicioAssinatura) + (original == 'A' ? 'B' : 'A')
                + token.substring(inicioAssinatura + 1);

        mockMvc.perform(get("/api/auth/me").with(token(adulterado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Token inválido ou expirado."));
        mockMvc.perform(get("/api/auth/me").with(token("nao-e-um-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoEhRejeitado() throws Exception {
        criarCliente("Ana", "ana@email.com");
        Usuario ana = usuarioRepository.findByEmail("ana@email.com").orElseThrow();
        // Emitido há 2 dias, com validade de 60 minutos
        String expirado = jwtService.gerarToken(ana, Instant.now().minus(2, ChronoUnit.DAYS)).token();

        mockMvc.perform(get("/api/auth/me").with(token(expirado)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("Token inválido ou expirado."));
    }

    @Test
    void tokenDeUsuarioExcluidoDeixaDeValer() throws Exception {
        String tokenAdmin = tokenDeAdmin();
        long idAna = criarCliente("Ana", "ana@email.com");
        String tokenAna = login("ana@email.com");

        mockMvc.perform(delete("/api/usuarios/" + idAna).with(token(tokenAdmin))).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").with(token(tokenAna)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("O usuário do token não existe mais."));
    }

    @Test
    void logoutRegistraASaida() throws Exception {
        String token = tokenDeCliente("Ana", "ana@email.com");

        mockMvc.perform(post("/api/auth/logout").with(token(token))).andExpect(status().isNoContent());

        assertThat(logRepository.findAllByOrderByIdDesc().get(0).getAcao()).isEqualTo("LOGOUT");
    }
}
