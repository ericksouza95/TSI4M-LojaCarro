package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.repository.VeiculoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Base dos testes de integração: sobe a aplicação inteira com banco H2 (schema criado pelo Flyway)
@SpringBootTest
@AutoConfigureMockMvc
@Import({RelogioFixoTestConfig.class, AuditoriaTestConfig.class})
abstract class IntegracaoTestBase {

    protected static final String SENHA = "123456";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected AgendamentoRepository agendamentoRepository;

    @Autowired
    protected VeiculoRepository veiculoRepository;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    // Eventos de auditoria que seriam enviados ao RabbitMQ
    @Autowired
    protected PublicadorEmMemoria auditoria;

    @Value("${loja.imagens.diretorio}")
    private String diretorioImagens;

    @BeforeEach
    void limparBanco() throws IOException {
        agendamentoRepository.deleteAll();
        veiculoRepository.deleteAll();
        usuarioRepository.deleteAll();
        auditoria.limpar();
        for (Path arquivo : arquivosDeImagem()) {
            Files.delete(arquivo);
        }
    }

    protected Path pastaDeImagens() {
        return Paths.get(diretorioImagens).toAbsolutePath().normalize();
    }

    protected List<Path> arquivosDeImagem() throws IOException {
        if (!Files.isDirectory(pastaDeImagens())) {
            return List.of();
        }
        try (Stream<Path> arquivos = Files.list(pastaDeImagens())) {
            return arquivos.toList();
        }
    }

    // Adiciona o cabeçalho "Authorization: Bearer <token>" na requisição
    protected static RequestPostProcessor token(String token) {
        return requisicao -> {
            requisicao.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return requisicao;
        };
    }

    // Administradores não podem ser criados pela API pública, então são inseridos direto no banco
    protected long criarAdmin(String nome, String email) {
        Usuario admin = new Usuario(nome, email, passwordEncoder.encode(SENHA), Perfil.ADMIN);
        return usuarioRepository.save(admin).getId();
    }

    protected long criarCliente(String nome, String email) throws Exception {
        String json = "{\"nome\":\"" + nome + "\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}";
        String resposta = mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }

    protected String login(String email) throws Exception {
        String resposta = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("token").asText();
    }

    protected String tokenDeAdmin() throws Exception {
        criarAdmin("Admin", "admin@loja.com");
        return login("admin@loja.com");
    }

    protected String tokenDeCliente(String nome, String email) throws Exception {
        criarCliente(nome, email);
        return login(email);
    }

    protected static String veiculoJson(String marca, String modelo, int anoFabricacao, int anoModelo, String preco) {
        return """
                {"marca":"%s","modelo":"%s","anoFabricacao":%d,"anoModelo":%d,"cor":"Prata",
                 "quilometragem":35000,"combustivel":"FLEX","transmissao":"AUTOMATICA",
                 "preco":%s,"descricao":"Único dono, revisões em dia"}
                """.formatted(marca, modelo, anoFabricacao, anoModelo, preco);
    }

    // Assinatura de um arquivo PNG (os primeiros bytes são o que o servidor usa para validar o tipo)
    protected static final byte[] BYTES_PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52};

    protected static MockMultipartFile fotoPng() {
        return new MockMultipartFile("foto", "carro.png", "image/png", BYTES_PNG);
    }

    // Parte "dados" do multipart, com o JSON do veículo
    protected static MockMultipartFile dados(String json) {
        return new MockMultipartFile("dados", "", MediaType.APPLICATION_JSON_VALUE, json.getBytes());
    }

    protected long cadastrarVeiculo(String tokenAdmin, String json) throws Exception {
        String resposta = mockMvc.perform(multipart("/api/veiculos").file(dados(json)).file(fotoPng())
                        .with(token(tokenAdmin)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(resposta).get("id").asLong();
    }
}
