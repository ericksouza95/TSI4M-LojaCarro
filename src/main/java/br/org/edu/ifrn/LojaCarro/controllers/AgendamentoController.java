package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.dto.AgendamentoFiltro;
import br.org.edu.ifrn.LojaCarro.dto.AgendamentoRequest;
import br.org.edu.ifrn.LojaCarro.dto.AgendamentoResponse;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.services.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agendamentos")
@Tag(name = "Agendamentos", description = "Test drives: agendamento, consulta e cancelamento")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;

    public AgendamentoController(AgendamentoService agendamentoService) {
        this.agendamentoService = agendamentoService;
    }

    @PostMapping
    @Operation(summary = "Agenda um test drive de 30 minutos (cliente)")
    public ResponseEntity<AgendamentoResponse> agendar(@Valid @RequestBody AgendamentoRequest dados) {
        AgendamentoResponse criado = agendamentoService.agendar(dados);
        return ResponseEntity.created(URI.create("/api/agendamentos/" + criado.id())).body(criado);
    }

    @GetMapping("/meus")
    @Operation(summary = "Agendamentos do cliente logado")
    public List<AgendamentoResponse> meus() {
        return agendamentoService.meusAgendamentos();
    }

    @GetMapping
    @Operation(summary = "Todos os agendamentos, com filtros por data, veículo e situação (administrador)")
    public List<AgendamentoResponse> listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                            @RequestParam(required = false) Long veiculoId,
                                            @RequestParam(required = false) StatusAgendamento status) {
        return agendamentoService.listar(new AgendamentoFiltro(data, veiculoId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhes de um agendamento (dono ou administrador)")
    public AgendamentoResponse buscarPorId(@PathVariable Long id) {
        return agendamentoService.buscarPorId(id);
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela um agendamento futuro e libera o horário")
    public AgendamentoResponse cancelar(@PathVariable Long id) {
        return agendamentoService.cancelar(id);
    }

    @PatchMapping("/{id}/realizado")
    @Operation(summary = "Marca o test drive como realizado (administrador)")
    public AgendamentoResponse marcarComoRealizado(@PathVariable Long id) {
        return agendamentoService.marcarComoRealizado(id);
    }
}
