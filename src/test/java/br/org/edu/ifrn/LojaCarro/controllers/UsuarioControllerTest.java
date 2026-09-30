package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.repository.LogRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void limparBanco() {
        usuarioRepository.deleteAll();
        logRepository.deleteAll();
    }

    private long criarUsuario(String nome, String email, String senha) throws Exception {
        String json = "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
        String resposta = mockMvc.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(resposta);
        return node.get("id").asLong();
    }

    @Test
    void crudCompletoDeUsuario() throws Exception {
        long id = criarUsuario("Maria", "maria@email.com", "123456");

        // A senha nunca deve ser devolvida
        mockMvc.perform(get("/usuario/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        mockMvc.perform(put("/usuario/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria Silva\",\"email\":\"maria@email.com\",\"senha\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Silva"));

        mockMvc.perform(get("/usuario"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/usuario/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/usuario/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void naoPermiteEmailDuplicado() throws Exception {
        criarUsuario("Joao", "joao@email.com", "123456");

        mockMvc.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outro\",\"email\":\"joao@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("joao@email.com")));
    }

    @Test
    void naoPermiteSenhaCurta() throws Exception {
        mockMvc.perform(post("/usuario").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@email.com\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registraLogsDasAcoes() throws Exception {
        long id = criarUsuario("Pedro", "pedro@email.com", "123456");
        mockMvc.perform(delete("/usuario/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/usuario/" + id)).andExpect(status().isBadRequest());

        // Logs vêm do mais recente para o mais antigo
        mockMvc.perform(get("/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].acao").value("ERRO"))
                .andExpect(jsonPath("$[1].acao").value("EXCLUIR"))
                .andExpect(jsonPath("$[2].acao").value("CRIAR"))
                .andExpect(jsonPath("$[2].entidade").value("Usuario"));
    }
}
