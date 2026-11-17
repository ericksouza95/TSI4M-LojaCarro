package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.dto.LoginRequest;
import br.org.edu.ifrn.LojaCarro.dto.LoginResponse;
import br.org.edu.ifrn.LojaCarro.dto.UsuarioResponse;
import br.org.edu.ifrn.LojaCarro.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login (JWT), logout e usuário atual")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login com e-mail e senha; devolve o token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest dados) {
        return authService.login(dados);
    }

    @PostMapping("/logout")
    @Operation(summary = "Registra a saída (o frontend descarta o token)")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Dados do usuário dono do token")
    public UsuarioResponse usuarioAtual() {
        return authService.usuarioAtual();
    }
}
