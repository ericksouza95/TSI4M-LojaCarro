package br.org.edu.ifrn.LojaCarro.controllers;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.services.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Criar usuário
    @PostMapping
    public ResponseEntity<Usuario> salvarUsuario(@RequestBody Usuario u) {
        return ResponseEntity.ok(usuarioService.save(u));
    }

    // Atualizar usuário (por ID)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable Long id, @RequestBody Usuario u) {
        u.setId(id);
        return ResponseEntity.ok(usuarioService.update(u));
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
    @GetMapping
    public ResponseEntity<List<Usuario>> pesquisarTodosUsuarios() {
        return ResponseEntity.ok(usuarioService.findAll());
    }
}
