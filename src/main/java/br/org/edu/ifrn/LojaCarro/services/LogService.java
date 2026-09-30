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

    // Registra a ação no arquivo de log e no banco de dados
    public void registrar(String acao, String entidade, String descricao) {
        logger.info("[{}] {} - {}", acao, entidade, descricao);
        logRepository.save(new LogSistema(acao, entidade, descricao));
    }

    public List<LogSistema> findAll() {
        return logRepository.findAllByOrderByIdDesc();
    }
}
