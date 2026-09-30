package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.model.LogSistema;
import br.org.edu.ifrn.LojaCarro.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private SessaoService sessaoService;

    // Registra a ação, e quem a fez, no arquivo de log e no banco de dados
    public void registrar(String acao, String entidade, String descricao) {
        String usuario = sessaoService.usuarioLogado()
                .map(u -> u.getNome() + " (" + u.getEmail() + ")")
                .orElse("anônimo");
        logger.info("[{}] {} - {} - por {}", acao, entidade, descricao, usuario);
        logRepository.save(new LogSistema(acao, entidade, descricao, usuario));
    }

    public List<LogSistema> findAll() {
        return logRepository.findAllByOrderByIdDesc();
    }
}
