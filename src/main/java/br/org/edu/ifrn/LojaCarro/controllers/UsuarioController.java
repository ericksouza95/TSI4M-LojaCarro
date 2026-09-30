package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Salvar usuário (criar conta, não exige login)
    @PostMapping("salvar")
    public ResponseEntity<Usuario> salvarUsuario(@RequestBody Usuario u) {
        Usuario savedUsuario = usuarioService.save(u);
        return ResponseEntity.ok(savedUsuario);
    }

    // Atualizar usuário (por ID)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario u) {
        u.setId(id);  // Define o ID no objeto
        Usuario updatedUsuario = usuarioService.update(u);
        return ResponseEntity.ok(updatedUsuario);
    }

    // Deletar usuário (por ID)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id) {
        usuarioService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Pesquisar usuário por ID
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> pesquisarUsuarioPorId(@PathVariable Long id) {
        Optional<Usuario> usuario = usuarioService.findById(id);
        return usuario.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Pesquisar todos os usuários
    @GetMapping("/listarUsuarios")
    public ResponseEntity<List<Usuario>> pesquisarTodosUsuarios() {
        List<Usuario> usuarios = usuarioService.findAll();
        return ResponseEntity.ok(usuarios);
    }

    // Login com e-mail e senha
    @PostMapping("/login")
    public ResponseEntity<Usuario> login(@RequestBody Usuario u) {
        return ResponseEntity.ok(usuarioService.login(u.getEmail(), u.getSenha()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        usuarioService.logout();
        return ResponseEntity.noContent().build();
    }

    // Retorna o usuário logado, ou 401 se ninguém fez login
    @GetMapping("/logado")
    public ResponseEntity<Usuario> usuarioLogado() {
        return usuarioService.usuarioLogado()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
