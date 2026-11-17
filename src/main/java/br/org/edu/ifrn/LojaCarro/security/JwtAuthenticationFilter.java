package br.org.edu.ifrn.LojaCarro.security;

import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

// Lê o cabeçalho "Authorization: Bearer <token>", valida o JWT e autentica a requisição.
// Sem token (ou com token inválido) a requisição segue anônima: as rotas protegidas
// serão barradas pelo Spring Security com 401.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Motivo da falha, usado pelo ErrosSegurancaHandler para montar a mensagem do 401
    public static final String ATRIBUTO_ERRO_TOKEN = "loja.erroToken";

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho != null && cabecalho.startsWith(PREFIXO)) {
            autenticar(cabecalho.substring(PREFIXO.length()).trim(), request);
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        Optional<Long> usuarioId = jwtService.extrairUsuarioId(token);
        if (usuarioId.isEmpty()) {
            request.setAttribute(ATRIBUTO_ERRO_TOKEN, "Token inválido ou expirado.");
            return;
        }
        // O usuário é recarregado do banco: se foi excluído, o token deixa de valer
        usuarioRepository.findById(usuarioId.get()).ifPresentOrElse(usuario -> {
            UsuarioAutenticado principal = UsuarioAutenticado.de(usuario);
            var autenticacao = new UsernamePasswordAuthenticationToken(principal, null, principal.autoridades());
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        }, () -> request.setAttribute(ATRIBUTO_ERRO_TOKEN, "O usuário do token não existe mais."));
    }
}
