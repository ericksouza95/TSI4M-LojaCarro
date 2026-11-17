package br.org.edu.ifrn.LojaCarro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.util.TimeZone;

// UserDetailsServiceAutoConfiguration é excluída porque a autenticação é feita pelo filtro JWT próprio
// (sem ela o Spring criaria um usuário "user" com senha gerada)
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LojaCarroApplication {

	public static void main(String[] args) {
		// Horários de test drive são da loja, não do servidor (a instância EC2 roda em UTC)
		String fuso = System.getenv().getOrDefault("LOJA_FUSO_HORARIO", "America/Fortaleza");
		TimeZone.setDefault(TimeZone.getTimeZone(fuso));
		SpringApplication.run(LojaCarroApplication.class, args);
	}
}
