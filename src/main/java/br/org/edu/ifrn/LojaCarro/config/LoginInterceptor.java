package br.org.edu.ifrn.LojaCarro.config;

import br.org.edu.ifrn.LojaCarro.services.LogService;
import br.org.edu.ifrn.LojaCarro.services.SessaoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Bloqueia as rotas do sistema para quem não fez login
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private SessaoService sessaoService;

    @Autowired
    private LogService logService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (sessaoService.usuarioLogado().isPresent()) {
            return true;
        }
        logService.registrar("ACESSO_NEGADO", "Sistema",
                "Tentativa de acesso sem login: " + request.getMethod() + " " + request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"erro\":\"É necessário fazer login.\"}");
        return false;
    }
}
