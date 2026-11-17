package br.org.edu.ifrn.LojaCarro.security;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

// Gera e valida os tokens JWT (assinatura HMAC-SHA256)
@Service
public class JwtService {

    private static final String EMISSOR = "lojacarro-api";
    private static final int TAMANHO_MINIMO_SEGREDO = 32; // 256 bits, exigido pelo HS256

    private final SecretKey chave;
    private final Duration validade;

    public JwtService(@Value("${loja.jwt.segredo}") String segredo,
                      @Value("${loja.jwt.expiracao-minutos:120}") long expiracaoMinutos) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalStateException("JWT_SECRET deve ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.validade = Duration.ofMinutes(expiracaoMinutos);
    }

    public TokenGerado gerarToken(Usuario usuario) {
        return gerarToken(usuario, Instant.now());
    }

    public TokenGerado gerarToken(Usuario usuario, Instant emitidoEm) {
        Instant expiraEm = emitidoEm.plus(validade);
        String token = Jwts.builder()
                .issuer(EMISSOR)
                .subject(String.valueOf(usuario.getId()))
                .claim("nome", usuario.getNome())
                .claim("email", usuario.getEmail())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(emitidoEm))
                .expiration(Date.from(expiraEm))
                .signWith(chave)
                .compact();
        return new TokenGerado(token, expiraEm);
    }

    // Retorna o ID do usuário se o token for autêntico, do emissor correto e não estiver expirado
    public Optional<Long> extrairUsuarioId(String token) {
        try {
            String subject = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(EMISSOR)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.of(Long.valueOf(subject));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record TokenGerado(String token, Instant expiraEm) {
    }
}
