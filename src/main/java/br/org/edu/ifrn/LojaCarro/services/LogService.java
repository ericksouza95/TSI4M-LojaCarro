package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.LogResponse;
import br.org.edu.ifrn.LojaCarro.mensageria.EventoAuditoria;
import br.org.edu.ifrn.LojaCarro.mensageria.LogsClient;
import br.org.edu.ifrn.LojaCarro.mensageria.PublicadorEventos;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

// Auditoria (RF07): cada operação gera um evento que é enviado pelo RabbitMQ ao microserviço de logs
@Service
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    public static final String ORIGEM = "lojacarro-api";

    private final PublicadorEventos publicador;
    private final LogsClient logsClient;
    private final UsuarioLogado usuarioLogado;

    public LogService(PublicadorEventos publicador, LogsClient logsClient, UsuarioLogado usuarioLogado) {
        this.publicador = publicador;
        this.logsClient = logsClient;
        this.usuarioLogado = usuarioLogado;
    }

    // Registra a ação feita pelo usuário autenticado na requisição (ou "anônimo")
    public void registrar(String acao, String entidade, String descricao) {
        String autor = usuarioLogado.atual().map(UsuarioAutenticado::descricao).orElse("anônimo");
        registrar(acao, entidade, descricao, autor);
    }

    // Usado quando o autor ainda não está no contexto de segurança (ex.: no momento do login)
    public void registrar(String acao, String entidade, String descricao, String autor) {
        EventoAuditoria evento = new EventoAuditoria(LocalDateTime.now(), acao, entidade, descricao, autor, ORIGEM);
        logger.info("[{}] {} - {} - por {}", acao, entidade, descricao, autor);

        // Dentro de uma transação, o evento só é enviado depois do commit:
        // se a operação for desfeita, não fica registrado algo que não aconteceu.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publicador.publicar(evento);
                }
            });
        } else {
            publicador.publicar(evento);
        }
    }

    public List<LogResponse> listar(String busca) {
        return logsClient.listar(busca);
    }
}
