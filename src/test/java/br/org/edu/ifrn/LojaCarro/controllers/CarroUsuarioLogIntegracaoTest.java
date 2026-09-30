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

// Verifica a integração entre Carro, Usuário (login) e Logs
@SpringBootTest
@AutoConfigureMockMvc
class CarroUsuarioLogIntegracaoTest {

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

    private MockHttpSession sessao;
    private long idUsuario;

    @BeforeEach
    void prepararUsuarioLogado() throws Exception {
        carroRepository.deleteAll();
        usuarioRepository.deleteAll();
        logRepository.deleteAll();

        String resposta = mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Maria\",\"email\":\"maria@email.com\",\"senha\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString();
        idUsuario = objectMapper.readTree(resposta).get("id").asLong();

        sessao = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@email.com\",\"senha\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    private long salvarCarro(String modelo) throws Exception {
        String resposta = mockMvc.perform(post("/carro/salvar").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelo\":\"" + modelo + "\",\"ano\":2020,\"preco\":50000}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    @Test
    void carroGuardaOUsuarioQueCadastrou() throws Exception {
        long idCarro = salvarCarro("Gol");

        mockMvc.perform(get("/carro/" + idCarro).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastradoPor.email").value("maria@email.com"))
                .andExpect(jsonPath("$.cadastradoPor.senha").doesNotExist());

        // Atualizar o carro não perde quem o cadastrou
        mockMvc.perform(put("/carro/" + idCarro).session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelo\":\"Uno\",\"ano\":2021,\"preco\":40000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cadastradoPor.email").value("maria@email.com"));
    }

    @Test
    void naoExcluiUsuarioComCarrosCadastrados() throws Exception {
        salvarCarro("Gol");

        // Outro usuário tenta excluir a Maria, que tem um carro
        mockMvc.perform(post("/usuario/salvar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"senha\":\"123456\"}"));
        MockHttpSession sessaoJoao = new MockHttpSession();
        mockMvc.perform(post("/usuario/login").session(sessaoJoao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"joao@email.com\",\"senha\":\"123456\"}"));

        mockMvc.perform(delete("/usuario/" + idUsuario).session(sessaoJoao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("possui carros cadastrados")));
    }

    @Test
    void logsRegistramQuemFezCadaAcao() throws Exception {
        salvarCarro("Gol");
        mockMvc.perform(post("/carro/salvar").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"modelo\":\"Modelo Grande\",\"ano\":2020,\"preco\":1}"))
                .andExpect(status().isBadRequest());

        // Logs vêm do mais recente para o mais antigo
        mockMvc.perform(get("/log").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].acao").value("ERRO"))
                .andExpect(jsonPath("$[0].entidade").value("Carro"))
                .andExpect(jsonPath("$[0].usuario").value("Maria (maria@email.com)"))
                .andExpect(jsonPath("$[1].acao").value("CRIAR"))
                .andExpect(jsonPath("$[1].entidade").value("Carro"))
                .andExpect(jsonPath("$[1].usuario").value("Maria (maria@email.com)"))
                .andExpect(jsonPath("$[2].acao").value("LOGIN"))
                .andExpect(jsonPath("$[3].acao").value("CRIAR"))
                .andExpect(jsonPath("$[3].usuario").value("anônimo"));
    }

    @Test
    void acessoSemLoginFicaRegistradoNoLog() throws Exception {
        mockMvc.perform(get("/carro/listarCarros")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/log").session(sessao))
                .andExpect(jsonPath("$[0].acao").value("ACESSO_NEGADO"))
                .andExpect(jsonPath("$[0].descricao").value(containsString("/carro/listarCarros")));
    }
}
