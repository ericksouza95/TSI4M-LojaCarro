package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.mensageria.EventoAuditoria;
import br.org.edu.ifrn.LojaCarro.mensageria.LogsClient;
import br.org.edu.ifrn.LojaCarro.mensageria.PublicadorEventos;
import br.org.edu.ifrn.LojaCarro.model.Perfil;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private PublicadorEventos publicador;

    @Mock
    private LogsClient logsClient;

    @Mock
    private UsuarioLogado usuarioLogado;

    @InjectMocks
    private LogService logService;

    @AfterEach
    void encerrarTransacaoSimulada() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void eventoLevaUsuarioAcaoEDetalhes() {
        when(usuarioLogado.atual()).thenReturn(Optional.of(new UsuarioAutenticado(1L, "Ana", "ana@email.com", Perfil.CLIENTE)));

        logService.registrar("CANCELAR", "Agendamento", "Test drive cancelado: ID 5");

        ArgumentCaptor<EventoAuditoria> evento = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(publicador).publicar(evento.capture());
        assertThat(evento.getValue().usuario()).isEqualTo("Ana (ana@email.com)");
        assertThat(evento.getValue().acao()).isEqualTo("CANCELAR");
        assertThat(evento.getValue().descricao()).isEqualTo("Test drive cancelado: ID 5");
        assertThat(evento.getValue().origem()).isEqualTo("lojacarro-api");
        assertThat(evento.getValue().dataHora()).isNotNull();
    }

    @Test
    void semUsuarioLogadoOAutorEhAnonimo() {
        when(usuarioLogado.atual()).thenReturn(Optional.empty());

        logService.registrar("ACESSO_NEGADO", "Sistema", "GET /api/usuarios");

        verify(publicador).publicar(argThatUsuario("anônimo"));
    }

    @Test
    void dentroDeTransacaoSoPublicaDepoisDoCommit() {
        TransactionSynchronizationManager.initSynchronization();

        logService.registrar("CRIAR", "Veiculo", "Veículo cadastrado", "Admin");
        verify(publicador, never()).publicar(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(publicador).publicar(any());
    }

    @Test
    void transacaoDesfeitaNaoPublicaOEvento() {
        TransactionSynchronizationManager.initSynchronization();

        logService.registrar("CRIAR", "Veiculo", "Veículo cadastrado", "Admin");
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(publicador, never()).publicar(any());
    }

    private static EventoAuditoria argThatUsuario(String usuario) {
        return argThat(e -> e.usuario().equals(usuario));
    }
}
