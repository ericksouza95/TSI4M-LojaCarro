package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.mensageria.EventoAuditoria;
import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VeiculoControllerTest extends IntegracaoTestBase {

    private String tokenAdmin;

    @BeforeEach
    void prepararAdmin() throws Exception {
        tokenAdmin = tokenDeAdmin();
    }

    @Test
    void administradorCadastraVeiculoEVisitanteConsultaOsDetalhes() throws Exception {
        mockMvc.perform(multipart("/api/veiculos").file(dados(veiculoJson("Toyota", "Corolla", 2022, 2023, "125990.90")))
                        .file(fotoPng()).with(token(tokenAdmin)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/veiculos/\\d+")))
                .andExpect(jsonPath("$.marca").value("Toyota"))
                .andExpect(jsonPath("$.situacao").value("DISPONIVEL"))
                .andExpect(jsonPath("$.preco").value(125990.90))
                .andExpect(jsonPath("$.fotoUrl").value(matchesPattern("/api/veiculos/\\d+/foto\\?v=[0-9a-f-]{8}")));

        long id = veiculoRepository.findAll().get(0).getId();

        // Detalhes são públicos (sem token) e não expõem dados de quem cadastrou
        mockMvc.perform(get("/api/veiculos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Corolla"))
                .andExpect(jsonPath("$.anoFabricacao").value(2022))
                .andExpect(jsonPath("$.anoModelo").value(2023))
                .andExpect(jsonPath("$.combustivel").value("FLEX"))
                .andExpect(jsonPath("$.cadastradoPor").doesNotExist());
    }

    @Test
    void cadastroDeVeiculoExigeLoginEPerfilAdministrador() throws Exception {
        String json = veiculoJson("Fiat", "Uno", 2015, 2015, "30000");

        mockMvc.perform(multipart("/api/veiculos").file(dados(json)).file(fotoPng()))
                .andExpect(status().isUnauthorized());

        String tokenCliente = tokenDeCliente("Ana", "ana@email.com");
        mockMvc.perform(multipart("/api/veiculos").file(dados(json)).file(fotoPng()).with(token(tokenCliente)))
                .andExpect(status().isForbidden());

        assertThat(veiculoRepository.count()).isZero();
    }

    @Test
    void clienteNaoAlteraNemDesativaVeiculo() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("VW", "Polo", 2023, 2023, "89000"));
        String tokenCliente = tokenDeCliente("Ana", "ana@email.com");

        mockMvc.perform(put("/api/veiculos/" + id).with(token(tokenCliente)).contentType(MediaType.APPLICATION_JSON)
                        .content(veiculoJson("VW", "Polo", 2023, 2023, "1")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/veiculos/" + id + "/situacao").with(token(tokenCliente))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"VENDIDO\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/veiculos/" + id).with(token(tokenCliente)))
                .andExpect(status().isForbidden());

        assertThat(veiculoRepository.findById(id)).get()
                .extracting("situacao").isEqualTo(SituacaoVeiculo.DISPONIVEL);
    }

    @Test
    void validaCamposObrigatorios() throws Exception {
        mockMvc.perform(multipart("/api/veiculos").file(dados("{\"marca\":\"\",\"preco\":-10,\"quilometragem\":-1}"))
                        .file(fotoPng()).with(token(tokenAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Dados inválidos."))
                .andExpect(jsonPath("$.campos.marca").exists())
                .andExpect(jsonPath("$.campos.modelo").exists())
                .andExpect(jsonPath("$.campos.preco").value("O preço deve ser maior que zero."))
                .andExpect(jsonPath("$.campos.quilometragem").exists())
                .andExpect(jsonPath("$.campos.combustivel").exists());
        assertThat(veiculoRepository.count()).isZero();
    }

    @Test
    void rejeitaAnoDoModeloAnteriorAoDeFabricacao() throws Exception {
        mockMvc.perform(multipart("/api/veiculos").file(dados(veiculoJson("Honda", "Civic", 2020, 2019, "90000")))
                        .file(fotoPng()).with(token(tokenAdmin)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("ano do modelo")));
        assertThat(arquivosDeImagem()).isEmpty();
    }

    // ---------- Fotografias ----------

    @Test
    void cadastroSemFotografiaEhRecusado() throws Exception {
        mockMvc.perform(multipart("/api/veiculos").file(dados(veiculoJson("Fiat", "Mobi", 2022, 2022, "60000")))
                        .with(token(tokenAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A fotografia do veículo é obrigatória."));

        // JSON puro (sem multipart) também não é aceito
        mockMvc.perform(post("/api/veiculos").with(token(tokenAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content(veiculoJson("Fiat", "Mobi", 2022, 2022, "60000")))
                .andExpect(status().isUnsupportedMediaType());
        assertThat(veiculoRepository.count()).isZero();
    }

    @Test
    void arquivoQueNaoEhImagemEhRecusadoMesmoComExtensaoJpg() throws Exception {
        MockMultipartFile pdfDisfarcado = new MockMultipartFile("foto", "carro.jpg", "image/jpeg",
                "%PDF-1.7 conteudo qualquer".getBytes());

        mockMvc.perform(multipart("/api/veiculos").file(dados(veiculoJson("Fiat", "Mobi", 2022, 2022, "60000")))
                        .file(pdfDisfarcado).with(token(tokenAdmin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("não é uma imagem")));
        assertThat(veiculoRepository.count()).isZero();
        assertThat(arquivosDeImagem()).isEmpty();
    }

    @Test
    void imagemMaiorQue5MbEhRecusada() throws Exception {
        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(BYTES_PNG, 0, grande, 0, BYTES_PNG.length);
        MockMultipartFile fotoGrande = new MockMultipartFile("foto", "grande.png", "image/png", grande);

        mockMvc.perform(multipart("/api/veiculos").file(dados(veiculoJson("Fiat", "Mobi", 2022, 2022, "60000")))
                        .file(fotoGrande).with(token(tokenAdmin)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.erro").value("A imagem deve ter no máximo 5 MB."));
        assertThat(veiculoRepository.count()).isZero();
    }

    @Test
    void fotografiaEhGravadaNaPastaERecuperadaPelaApi() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Etios", 2019, 2019, "55000"));
        String arquivo = veiculoRepository.findById(id).orElseThrow().getFotoPrincipal();

        // Nome gerado pelo servidor (UUID), não o nome enviado pelo cliente
        assertThat(arquivo).matches("[0-9a-f-]{36}\\.png").isNotEqualTo("carro.png");
        assertThat(pastaDeImagens().resolve(arquivo)).exists();

        mockMvc.perform(get("/api/veiculos/" + id + "/foto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(BYTES_PNG));
    }

    @Test
    void administradorTrocaAFotoEAAntigaEhApagada() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Etios", 2019, 2019, "55000"));
        String antiga = veiculoRepository.findById(id).orElseThrow().getFotoPrincipal();
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F'};

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/veiculos/" + id + "/foto")
                        .file(new MockMultipartFile("foto", "nova.jpeg", "image/jpeg", jpeg)).with(token(tokenAdmin)))
                .andExpect(status().isOk());

        String nova = veiculoRepository.findById(id).orElseThrow().getFotoPrincipal();
        assertThat(nova).endsWith(".jpg").isNotEqualTo(antiga);
        assertThat(pastaDeImagens().resolve(antiga)).doesNotExist();
        mockMvc.perform(get("/api/veiculos/" + id + "/foto"))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(jpeg));
    }

    @Test
    void clienteNaoTrocaFotoDeVeiculo() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Etios", 2019, 2019, "55000"));
        String tokenCliente = tokenDeCliente("Ana", "ana@email.com");

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/veiculos/" + id + "/foto").file(fotoPng())
                        .with(token(tokenCliente)))
                .andExpect(status().isForbidden());
    }

    @Test
    void fotoDeVeiculoInativoNaoEhPublica() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Etios", 2019, 2019, "55000"));
        mockMvc.perform(delete("/api/veiculos/" + id).with(token(tokenAdmin)));

        mockMvc.perform(get("/api/veiculos/" + id + "/foto")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/veiculos/" + id + "/foto").with(token(tokenAdmin))).andExpect(status().isOk());
    }

    @Test
    void pesquisaPorMarcaOuModeloEFiltraPorPrecoEAno() throws Exception {
        cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Corolla", 2022, 2023, "125000"));
        cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Yaris", 2019, 2019, "70000"));
        cadastrarVeiculo(tokenAdmin, veiculoJson("Honda", "Civic", 2018, 2018, "95000"));

        mockMvc.perform(get("/api/veiculos").param("busca", "toyota"))
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/api/veiculos").param("busca", "civ"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].modelo").value("Civic"));
        mockMvc.perform(get("/api/veiculos").param("precoMin", "80000").param("precoMax", "130000"))
                .andExpect(jsonPath("$[*].modelo", containsInAnyOrder("Corolla", "Civic")));
        mockMvc.perform(get("/api/veiculos").param("anoMin", "2019").param("anoMax", "2019"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].modelo").value("Yaris"));
        mockMvc.perform(get("/api/veiculos").param("precoMin", "100").param("precoMax", "50"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void exclusaoEhLogicaEOcultaOVeiculoDoPublico() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("Fiat", "Pulse", 2023, 2024, "110000"));

        mockMvc.perform(delete("/api/veiculos/" + id).with(token(tokenAdmin)))
                .andExpect(status().isNoContent());

        // Continua no banco, apenas inativo
        assertThat(veiculoRepository.findById(id)).get()
                .extracting("situacao").isEqualTo(SituacaoVeiculo.INATIVO);

        // Visitante: não aparece na lista, nem pedindo explicitamente, e o detalhe responde 404
        mockMvc.perform(get("/api/veiculos")).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/veiculos").param("situacao", "INATIVO")).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/veiculos/" + id)).andExpect(status().isNotFound());

        // Administrador continua vendo
        mockMvc.perform(get("/api/veiculos").with(token(tokenAdmin)).param("situacao", "INATIVO"))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/api/veiculos/" + id).with(token(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("INATIVO"));
    }

    @Test
    void marcaVeiculoComoVendido() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("Jeep", "Renegade", 2021, 2021, "98000"));

        mockMvc.perform(patch("/api/veiculos/" + id + "/situacao").with(token(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"VENDIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("VENDIDO"));

        mockMvc.perform(get("/api/veiculos")).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/veiculos").with(token(tokenAdmin)).param("situacao", "VENDIDO", "DISPONIVEL"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void atualizaVeiculoERetorna404ParaInexistente() throws Exception {
        long id = cadastrarVeiculo(tokenAdmin, veiculoJson("VW", "Gol", 2016, 2017, "38000"));

        mockMvc.perform(put("/api/veiculos/" + id).with(token(tokenAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content(veiculoJson("VW", "Gol", 2016, 2017, "35500")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(35500));

        mockMvc.perform(put("/api/veiculos/999999").with(token(tokenAdmin)).contentType(MediaType.APPLICATION_JSON)
                        .content(veiculoJson("VW", "Gol", 2016, 2017, "35500")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/veiculos/999999")).andExpect(status().isNotFound());
    }

    @Test
    void operacoesFicamRegistradasNoLogComOAutor() throws Exception {
        cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Hilux", 2020, 2020, "210000"));

        // Evento enviado ao microserviço de logs: horário, usuário, ação e detalhes
        EventoAuditoria evento = auditoria.ultimo();
        assertThat(evento.acao()).isEqualTo("CRIAR");
        assertThat(evento.entidade()).isEqualTo("Veiculo");
        assertThat(evento.descricao()).contains("Toyota Hilux");
        assertThat(evento.usuario()).isEqualTo("Admin (admin@loja.com)");
        assertThat(evento.origem()).isEqualTo("lojacarro-api");
        assertThat(evento.dataHora()).isNotNull();
    }
}
