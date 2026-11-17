package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// "Agora" nos testes: segunda-feira, 04/03/2030, 09:00 (ver RelogioFixoTestConfig)
class AgendamentoControllerTest extends IntegracaoTestBase {

    private static final LocalDate HOJE = LocalDate.of(2030, 3, 4);       // segunda
    private static final LocalDate TERCA = LocalDate.of(2030, 3, 5);
    private static final LocalDate SABADO = LocalDate.of(2030, 3, 9);
    private static final LocalDate DOMINGO = LocalDate.of(2030, 3, 10);

    private String tokenAdmin;
    private String tokenAna;
    private String tokenBruno;
    private long corolla;
    private long civic;

    @BeforeEach
    void prepararDados() throws Exception {
        tokenAdmin = tokenDeAdmin();
        tokenAna = tokenDeCliente("Ana", "ana@email.com");
        tokenBruno = tokenDeCliente("Bruno", "bruno@email.com");
        corolla = cadastrarVeiculo(tokenAdmin, veiculoJson("Toyota", "Corolla", 2023, 2024, "135000"));
        civic = cadastrarVeiculo(tokenAdmin, veiculoJson("Honda", "Civic", 2022, 2022, "120000"));
    }

    private ResultActions agendar(String token, long veiculoId, LocalDate data, String horario) throws Exception {
        String json = "{\"veiculoId\":" + veiculoId + ",\"data\":\"" + data + "\",\"horario\":\"" + horario + "\"}";
        return mockMvc.perform(post("/api/agendamentos").with(token(token))
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long idDe(ResultActions resultado) throws Exception {
        return objectMapper.readTree(resultado.andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    // ---------- Agendamento ----------

    @Test
    void clienteAgendaTestDriveDe30Minutos() throws Exception {
        agendar(tokenAna, corolla, TERCA, "14:00")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/agendamentos/\\d+")))
                .andExpect(jsonPath("$.veiculo").value("Toyota Corolla 2023/2024"))
                .andExpect(jsonPath("$.clienteNome").value("Ana"))
                .andExpect(jsonPath("$.data").value("2030-03-05"))
                .andExpect(jsonPath("$.horarioInicio").value("14:00"))
                .andExpect(jsonPath("$.horarioFim").value("14:30"))
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.cancelavel").value(true))
                .andExpect(jsonPath("$.dataCriacao").exists());
    }

    // Exatamente o exemplo do roteiro (RF04)
    @Test
    void mesmoVeiculoNaoTemDoisAgendamentosEmHorariosCoincidentes() throws Exception {
        agendar(tokenAna, corolla, TERCA, "14:00").andExpect(status().isCreated());

        // Outro cliente NÃO pode agendar o mesmo Corolla às 14h nem às 14h15
        agendar(tokenBruno, corolla, TERCA, "14:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("já possui um test drive")));
        agendar(tokenBruno, corolla, TERCA, "14:15").andExpect(status().isConflict());
        // ...nem às 13h45 (terminaria às 14h15, dentro do horário da Ana)
        agendar(tokenBruno, corolla, TERCA, "13:45").andExpect(status().isConflict());

        // Outro cliente PODE agendar o mesmo Corolla às 15h e outro veículo às 14h
        agendar(tokenBruno, corolla, TERCA, "15:00").andExpect(status().isCreated());
        agendar(tokenBruno, civic, TERCA, "14:00").andExpect(status().isCreated());

        assertThat(agendamentoRepository.count()).isEqualTo(3);
    }

    @Test
    void horarioQueComecaQuandoOutroTerminaEhPermitido() throws Exception {
        agendar(tokenAna, corolla, TERCA, "14:00").andExpect(status().isCreated());

        agendar(tokenBruno, corolla, TERCA, "14:30").andExpect(status().isCreated());
        agendar(tokenBruno, corolla, TERCA, "13:30").andExpect(status().isCreated());
    }

    @Test
    void clienteNaoTemDoisTestDrivesAoMesmoTempo() throws Exception {
        agendar(tokenAna, corolla, TERCA, "10:00").andExpect(status().isCreated());

        agendar(tokenAna, civic, TERCA, "10:15")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(containsString("Você já possui")));
    }

    @Test
    void naoAgendaEmDataOuHorarioPassado() throws Exception {
        agendar(tokenAna, corolla, LocalDate.of(2030, 3, 1), "10:00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("passado")));
        // Hoje, mas antes das 09:00 (hora atual nos testes)
        agendar(tokenAna, corolla, HOJE, "08:30").andExpect(status().isUnprocessableEntity());
        agendar(tokenAna, corolla, HOJE, "09:30").andExpect(status().isCreated());
    }

    @Test
    void naoAgendaForaDoHorarioDeFuncionamento() throws Exception {
        agendar(tokenAna, corolla, TERCA, "07:30")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("fora do funcionamento")));
        // Começa 17:45 e terminaria 18:15, depois do fechamento
        agendar(tokenAna, corolla, TERCA, "17:45").andExpect(status().isUnprocessableEntity());
        agendar(tokenAna, corolla, DOMINGO, "10:00").andExpect(status().isUnprocessableEntity());
        agendar(tokenAna, corolla, SABADO, "12:00").andExpect(status().isUnprocessableEntity());

        agendar(tokenAna, corolla, TERCA, "17:30").andExpect(status().isCreated());
        agendar(tokenAna, corolla, SABADO, "11:30").andExpect(status().isCreated());
    }

    @Test
    void naoAgendaVeiculoVendidoOuInativo() throws Exception {
        mockMvc.perform(patch("/api/veiculos/" + corolla + "/situacao").with(token(tokenAdmin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"VENDIDO\"}"));
        mockMvc.perform(delete("/api/veiculos/" + civic).with(token(tokenAdmin)));

        agendar(tokenAna, corolla, TERCA, "10:00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value("Este veículo não está disponível para test drive."));
        agendar(tokenAna, civic, TERCA, "10:00").andExpect(status().isUnprocessableEntity());
        agendar(tokenAna, 999999, TERCA, "10:00").andExpect(status().isNotFound());
    }

    @Test
    void venderVeiculoCancelaOsTestDrivesFuturos() throws Exception {
        long agendamento = idDe(agendar(tokenAna, corolla, TERCA, "10:00"));

        mockMvc.perform(patch("/api/veiculos/" + corolla + "/situacao").with(token(tokenAdmin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"situacao\":\"VENDIDO\"}"))
                .andExpect(status().isOk());

        assertThat(agendamentoRepository.findById(agendamento)).get()
                .extracting(Agendamento::getStatus).isEqualTo(StatusAgendamento.CANCELADO);
    }

    @Test
    void agendamentoExigeLoginDeCliente() throws Exception {
        String json = "{\"veiculoId\":" + corolla + ",\"data\":\"" + TERCA + "\",\"horario\":\"10:00\"}";
        mockMvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isUnauthorized());
        agendar(tokenAdmin, corolla, TERCA, "10:00").andExpect(status().isForbidden());
    }

    @Test
    void validaCamposDoAgendamento() throws Exception {
        mockMvc.perform(post("/api/agendamentos").with(token(tokenAna)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.veiculoId").exists())
                .andExpect(jsonPath("$.campos.data").exists())
                .andExpect(jsonPath("$.campos.horario").exists());
    }

    // ---------- Cancelamento ----------

    @Test
    void cancelamentoLiberaOHorario() throws Exception {
        long id = idDe(agendar(tokenAna, corolla, TERCA, "14:00"));
        agendar(tokenBruno, corolla, TERCA, "14:00").andExpect(status().isConflict());

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").with(token(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.dataCancelamento").exists())
                .andExpect(jsonPath("$.cancelavel").value(false));

        agendar(tokenBruno, corolla, TERCA, "14:00").andExpect(status().isCreated());
    }

    @Test
    void clienteNaoCancelaAgendamentoDeOutroMasAdministradorSim() throws Exception {
        long id = idDe(agendar(tokenAna, corolla, TERCA, "14:00"));

        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").with(token(tokenBruno)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/agendamentos/" + id + "/cancelar").with(token(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void naoCancelaAgendamentoQueJaComecouNemOJaCancelado() throws Exception {
        long passado = agendamentoNoPassado();
        mockMvc.perform(patch("/api/agendamentos/" + passado + "/cancelar").with(token(tokenAna)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").value(containsString("ainda não começaram")));

        long futuro = idDe(agendar(tokenAna, corolla, TERCA, "14:00"));
        mockMvc.perform(patch("/api/agendamentos/" + futuro + "/cancelar").with(token(tokenAna)));
        mockMvc.perform(patch("/api/agendamentos/" + futuro + "/cancelar").with(token(tokenAna)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- Consultas ----------

    @Test
    void clienteConsultaApenasOsPropriosAgendamentos() throws Exception {
        agendar(tokenAna, corolla, TERCA, "10:00");
        agendar(tokenAna, civic, TERCA, "15:00");
        long doBruno = idDe(agendar(tokenBruno, corolla, TERCA, "16:00"));

        mockMvc.perform(get("/api/agendamentos/meus").with(token(tokenAna)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].clienteNome", everyItem(is("Ana"))));

        mockMvc.perform(get("/api/agendamentos/" + doBruno).with(token(tokenAna))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/agendamentos").with(token(tokenAna))).andExpect(status().isForbidden());
    }

    @Test
    void administradorConsultaAgendamentosPorDataEVeiculo() throws Exception {
        agendar(tokenAna, corolla, TERCA, "10:00");
        agendar(tokenBruno, civic, TERCA, "10:00");
        agendar(tokenAna, corolla, LocalDate.of(2030, 3, 6), "11:00");

        mockMvc.perform(get("/api/agendamentos").with(token(tokenAdmin)))
                .andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(get("/api/agendamentos").with(token(tokenAdmin)).param("data", "2030-03-05"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].clienteNome", containsInAnyOrder("Ana", "Bruno")));
        mockMvc.perform(get("/api/agendamentos").with(token(tokenAdmin))
                        .param("data", "2030-03-05").param("veiculoId", String.valueOf(corolla)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clienteNome").value("Ana"))
                .andExpect(jsonPath("$[0].clienteEmail").value("ana@email.com"))
                .andExpect(jsonPath("$[0].veiculo").value("Toyota Corolla 2023/2024"))
                .andExpect(jsonPath("$[0].horarioInicio").value("10:00"))
                .andExpect(jsonPath("$[0].status").value("CONFIRMADO"));
    }

    @Test
    void administradorMarcaTestDriveComoRealizado() throws Exception {
        long passado = agendamentoNoPassado();
        long futuro = idDe(agendar(tokenAna, corolla, TERCA, "10:00"));

        mockMvc.perform(patch("/api/agendamentos/" + passado + "/realizado").with(token(tokenAna)))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/agendamentos/" + passado + "/realizado").with(token(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADO"));
        mockMvc.perform(patch("/api/agendamentos/" + futuro + "/realizado").with(token(tokenAdmin)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---------- Grade de horários disponíveis ----------

    @Test
    void gradeMostraHorariosLivresEOcupadosDoDia() throws Exception {
        agendar(tokenAna, corolla, TERCA, "14:00");

        mockMvc.perform(get("/api/veiculos/" + corolla + "/horarios").param("data", TERCA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(20)))                // 08:00 até 17:30
                .andExpect(jsonPath("$[0].inicio").value("08:00"))
                .andExpect(jsonPath("$[19].inicio").value("17:30"))
                .andExpect(jsonPath("$[19].fim").value("18:00"))
                .andExpect(jsonPath("$[?(@.inicio == '14:00')].disponivel").value(contains(false)))
                .andExpect(jsonPath("$[?(@.inicio == '14:30')].disponivel").value(contains(true)))
                .andExpect(jsonPath("$[?(@.disponivel == false)]", hasSize(1)));

        // Outro veículo continua com o horário livre
        mockMvc.perform(get("/api/veiculos/" + civic + "/horarios").param("data", TERCA.toString()))
                .andExpect(jsonPath("$[?(@.disponivel == false)]", hasSize(0)));
    }

    @Test
    void gradeRespeitaHorarioDeFuncionamentoEHorariosJaPassados() throws Exception {
        mockMvc.perform(get("/api/veiculos/" + corolla + "/horarios").param("data", SABADO.toString()))
                .andExpect(jsonPath("$", hasSize(8)));               // 08:00 até 11:30
        mockMvc.perform(get("/api/veiculos/" + corolla + "/horarios").param("data", DOMINGO.toString()))
                .andExpect(jsonPath("$", hasSize(0)));
        // Hoje às 09:00: 08:00, 08:30 e 09:00 já passaram
        mockMvc.perform(get("/api/veiculos/" + corolla + "/horarios").param("data", HOJE.toString()))
                .andExpect(jsonPath("$[?(@.disponivel == false)].inicio", contains("08:00", "08:30", "09:00")));
    }

    // ---------- Concorrência ----------

    // Vários clientes tentam o MESMO horário do MESMO veículo ao mesmo tempo: só um pode conseguir
    @Test
    void pedidosSimultaneosParaOMesmoHorarioGeramApenasUmAgendamento() throws Exception {
        int quantidade = 8;
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            tokens.add(tokenDeCliente("Cliente " + i, "cliente" + i + "@email.com"));
        }

        ExecutorService executor = Executors.newFixedThreadPool(quantidade);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> respostas = new ArrayList<>();
        for (String token : tokens) {
            respostas.add(executor.submit(() -> {
                largada.await();
                return agendar(token, corolla, TERCA, "14:00").andReturn().getResponse().getStatus();
            }));
        }
        largada.countDown(); // todos disparam juntos

        List<Integer> status = new ArrayList<>();
        for (Future<Integer> resposta : respostas) {
            status.add(resposta.get(30, TimeUnit.SECONDS));
        }
        executor.shutdown();

        assertThat(status).filteredOn(s -> s == 201).hasSize(1);
        assertThat(status).filteredOn(s -> s == 409).hasSize(quantidade - 1);
        assertThat(agendamentoRepository.findAll())
                .filteredOn(a -> a.getStatus() == StatusAgendamento.CONFIRMADO)
                .hasSize(1);
    }

    // Insere direto no banco um agendamento de hoje às 08:00 (antes do "agora" dos testes), que a API não permitiria criar
    private long agendamentoNoPassado() {
        var ana = usuarioRepository.findByEmail("ana@email.com").orElseThrow();
        var veiculo = veiculoRepository.findById(corolla).orElseThrow();
        return agendamentoRepository.save(new Agendamento(ana, veiculo, LocalDateTime.of(HOJE, java.time.LocalTime.of(8, 0)))).getId();
    }
}
