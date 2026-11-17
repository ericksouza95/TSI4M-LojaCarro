package br.org.edu.ifrn.LojaCarro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration é excluída porque a autenticação é feita pelo filtro JWT próprio
// (sem ela o Spring criaria um usuário "user" com senha gerada)
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LojaCarroApplication {

	public static void main(String[] args) {
		SpringApplication.run(LojaCarroApplication.class, args);
	}
}
