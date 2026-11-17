package br.org.edu.ifrn.LojaCarro.config;

import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

// O cadastro público só cria clientes. O primeiro administrador é criado aqui, a partir de ADMIN_EMAIL e ADMIN_SENHA.
// Opcionalmente, também cria um cliente de demonstração (DEMO_CLIENTE_EMAIL e DEMO_CLIENTE_SENHA).
// Nada é criado se as variáveis não forem informadas ou se o e-mail já existir.
@Component
public class UsuariosIniciais implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(UsuariosIniciais.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminNome;
    private final String adminEmail;
    private final String adminSenha;
    private final String clienteEmail;
    private final String clienteSenha;

    public UsuariosIniciais(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                            @Value("${loja.admin.nome:Administrador}") String adminNome,
                            @Value("${loja.admin.email:}") String adminEmail,
                            @Value("${loja.admin.senha:}") String adminSenha,
                            @Value("${loja.demo.cliente-email:}") String clienteEmail,
                            @Value("${loja.demo.cliente-senha:}") String clienteSenha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminNome = adminNome;
        this.adminEmail = normalizar(adminEmail);
        this.adminSenha = adminSenha;
        this.clienteEmail = normalizar(clienteEmail);
        this.clienteSenha = clienteSenha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminSenha.isBlank()) {
            logger.info("ADMIN_EMAIL/ADMIN_SENHA não informados: nenhum administrador inicial foi criado.");
        } else {
            criarSeNaoExistir(adminNome, adminEmail, adminSenha, Perfil.ADMIN);
        }
        if (!clienteEmail.isBlank() && !clienteSenha.isBlank()) {
            criarSeNaoExistir("Cliente Demonstração", clienteEmail, clienteSenha, Perfil.CLIENTE);
        }
    }

    private void criarSeNaoExistir(String nome, String email, String senha, Perfil perfil) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(senha), perfil));
        logger.info("Usuário inicial criado ({}): {}", perfil, email);
    }

    private static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
