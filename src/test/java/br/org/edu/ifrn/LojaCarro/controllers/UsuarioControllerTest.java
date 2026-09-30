package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import br.org.edu.ifrn.LojaCarro.repository.LogRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparBanco() {
        carroRepository.deleteAll();
        usuarioRepository.deleteAll();
        logRepository.deleteAll();
    }

    private long criarUsuario(String nome, String email) throws Exception {
        String json = "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"123456\"}";
        String resposta = mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    private MockHttpSession logar(String email) throws Exception {
        MockHttpSession sessao = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk());
        return sessao;
    }

    @Test
    void crudCompletoDeUsuario() throws Exception {
        criarUsuario("Admin", "admin@email.com");
        MockHttpSession sessao = logar("admin@email.com");
        long id = criarUsuario("Maria", "maria@email.com");

        // A senha nunca deve ser devolvida
        mockMvc.perform(get("/usuario/" + id).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        mockMvc.perform(put("/usuario/" + id).session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Silva\",\"email\":\"maria@email.com\",\"senha\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));

        mockMvc.perform(get("/usuario/listarUsuarios").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(delete("/usuario/" + id).session(sessao))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/usuario/" + id).session(sessao))
                .andExpect(status().isNotFound());
    }

    @Test
    void exigeLoginParaAcessarOSistema() throws Exception {
        mockMvc.perform(get("/usuario/listarUsuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/carro/listarCarros")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/log")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/usuario/logado")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginComSenhaErradaFalha() throws Exception {
        criarUsuario("Joao", "joao@email.com");

        mockMvc.perform(post("/usuario/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"joao@email.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("E-mail ou senha inválidos."));
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        criarUsuario("Joao", "joao@email.com");
        MockHttpSession sessao = logar("joao@email.com");

        mockMvc.perform(get("/usuario/logado").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joao@email.com"));

        mockMvc.perform(post("/usuario/logout").session(sessao)).andExpect(status().isNoContent());
        mockMvc.perform(get("/usuario/listarUsuarios").session(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void naoPermiteEmailDuplicado() throws Exception {
        criarUsuario("Joao", "joao@email.com");

        mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro\",\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("joao@email.com")));
    }

    @Test
    void naoPermiteSenhaCurta() throws Exception {
        mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@email.com\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoPermiteExcluirOUsuarioLogado() throws Exception {
        long id = criarUsuario("Joao", "joao@email.com");
        MockHttpSession sessao = logar("joao@email.com");

        mockMvc.perform(delete("/usuario/" + id).session(sessao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Não é possível excluir o usuário que está logado."));
    }
}
