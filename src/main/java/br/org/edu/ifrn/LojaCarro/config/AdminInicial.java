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

// O cadastro público só cria clientes. O primeiro administrador é criado aqui,
// a partir das variáveis de ambiente ADMIN_EMAIL e ADMIN_SENHA (se informadas).
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AdminInicial(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                        @Value("${loja.admin.nome:Administrador}") String nome,
                        @Value("${loja.admin.email:}") String email,
                        @Value("${loja.admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || senha.isBlank()) {
            logger.info("ADMIN_EMAIL/ADMIN_SENHA não informados: nenhum administrador inicial foi criado.");
            return;
        }
        if (usuarioRepository.findByEmail(email).isPresent()) {
            return;
        }
        usuarioRepository.save(new Usuario(nome, email, passwordEncoder.encode(senha), Perfil.ADMIN));
        logger.info("Administrador inicial criado: {}", email);
    }
}
