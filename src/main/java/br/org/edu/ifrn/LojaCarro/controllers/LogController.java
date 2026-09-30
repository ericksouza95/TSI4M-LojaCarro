package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.LogSistema;
import br.org.edu.ifrn.LojaCarro.services.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private LogService logService;

    // Listar todos os logs (mais recentes primeiro)
    @GetMapping
    public ResponseEntity<List<LogSistema>> listarLogs() {
        return ResponseEntity.ok(logService.findAll());
    }
}
