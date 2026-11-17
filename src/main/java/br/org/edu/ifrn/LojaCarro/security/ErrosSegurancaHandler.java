package br.org.edu.ifrn.LojaCarro.security;

import br.org.edu.ifrn.LojaCarro.dto.ErroResponse;
import br.org.edu.ifrn.LojaCarro.services.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

// Respostas JSON para acesso sem autenticação (401) e sem permissão (403), com registro na auditoria
@Component
public class ErrosSegurancaHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final LogService logService;
    private final ObjectMapper objectMapper;

    public ErrosSegurancaHandler(LogService logService, ObjectMapper objectMapper) {
        this.logService = logService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        Object erroToken = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERRO_TOKEN);
        String mensagem = erroToken != null ? erroToken.toString() : "É necessário fazer login.";
        logService.registrar("ACESSO_NEGADO", "Sistema",
                "Sem autenticação válida: " + request.getMethod() + " " + request.getRequestURI() + " - " + mensagem);
        escrever(response, request, HttpStatus.UNAUTHORIZED, mensagem);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        logService.registrar("ACESSO_PROIBIDO", "Sistema",
                "Perfil sem permissão: " + request.getMethod() + " " + request.getRequestURI());
        escrever(response, request, HttpStatus.FORBIDDEN, "Você não tem permissão para realizar esta operação.");
    }

    private void escrever(HttpServletResponse response, HttpServletRequest request, HttpStatus status, String mensagem)
            throws IOException {
        ErroResponse corpo = new ErroResponse(status.value(), mensagem, null, request.getRequestURI(), LocalDateTime.now());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), corpo);
    }
}
