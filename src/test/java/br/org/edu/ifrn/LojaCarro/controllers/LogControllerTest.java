package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.dto.LogResponse;
import br.org.edu.ifrn.LojaCarro.exception.ServicoIndisponivelException;
import br.org.edu.ifrn.LojaCarro.mensageria.LogsClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// A tela de logs do administrador busca os dados no microserviço (aqui simulado)
class LogControllerTest extends IntegracaoTestBase {

    @MockitoBean
    private LogsClient logsClient;

    @Test
    void administradorConsultaOsLogsDoMicroservico() throws Exception {
        when(logsClient.listar("corolla")).thenReturn(List.of(new LogResponse(7L, LocalDateTime.of(2030, 3, 4, 9, 0),
                "CRIAR", "Veiculo", "Veículo cadastrado: Toyota Corolla", "Admin (admin@loja.com)", "lojacarro-api")));

        mockMvc.perform(get("/api/logs").param("busca", "corolla").with(token(tokenDeAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].acao").value("CRIAR"))
                .andExpect(jsonPath("$[0].usuario").value("Admin (admin@loja.com)"))
                .andExpect(jsonPath("$[0].origem").value("lojacarro-api"));
    }

    @Test
    void microservicoForaDoArRetorna503() throws Exception {
        when(logsClient.listar(any())).thenThrow(new ServicoIndisponivelException("O serviço de logs está indisponível no momento."));

        mockMvc.perform(get("/api/logs").with(token(tokenDeAdmin())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").value("O serviço de logs está indisponível no momento."));
    }
}
