package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.dto.UsuarioAtualizacaoRequest;
import br.org.edu.ifrn.LojaCarro.dto.UsuarioCadastroRequest;
import br.org.edu.ifrn.LojaCarro.dto.UsuarioResponse;
import br.org.edu.ifrn.LojaCarro.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuários", description = "Cadastro e gerenciamento de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(summary = "Cria uma conta de cliente (público)")
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody UsuarioCadastroRequest dados) {
        UsuarioResponse criado = usuarioService.cadastrarCliente(dados);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.id())).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista os usuários")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um usuário pelo ID")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um usuário")
    public UsuarioResponse atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioAtualizacaoRequest dados) {
        return usuarioService.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um usuário sem veículos nem agendamentos")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
