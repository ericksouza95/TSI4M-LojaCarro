package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.LogResponse;
import br.org.edu.ifrn.LojaCarro.model.LogSistema;
import br.org.edu.ifrn.LojaCarro.repository.LogRepository;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    private final LogRepository logRepository;
    private final UsuarioLogado usuarioLogado;

    public LogService(LogRepository logRepository, UsuarioLogado usuarioLogado) {
        this.logRepository = logRepository;
        this.usuarioLogado = usuarioLogado;
    }

    // Registra a ação feita pelo usuário autenticado na requisição (ou "anônimo")
    public void registrar(String acao, String entidade, String descricao) {
        String autor = usuarioLogado.atual().map(UsuarioAutenticado::descricao).orElse("anônimo");
        registrar(acao, entidade, descricao, autor);
    }

    // Usado quando o autor ainda não está no contexto de segurança (ex.: no momento do login)
    public void registrar(String acao, String entidade, String descricao, String autor) {
        logger.info("[{}] {} - {} - por {}", acao, entidade, descricao, autor);
        logRepository.save(new LogSistema(acao, entidade, descricao, autor));
    }

    @Transactional(readOnly = true)
    public List<LogResponse> listar() {
        return logRepository.findAllByOrderByIdDesc().stream().map(LogResponse::de).toList();
    }
}
