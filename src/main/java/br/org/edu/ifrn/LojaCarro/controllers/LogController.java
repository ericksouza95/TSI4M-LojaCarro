package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.dto.LogResponse;
import br.org.edu.ifrn.LojaCarro.services.LogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@Tag(name = "Logs", description = "Auditoria das operações (dados vindos do microserviço de logs)")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    @Operation(summary = "Lista os logs (mais recentes primeiro), com busca opcional")
    public List<LogResponse> listar(@RequestParam(required = false) String busca) {
        return logService.listar(busca);
    }
}
