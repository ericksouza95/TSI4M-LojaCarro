package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.config.HorarioFuncionamento;
import br.org.edu.ifrn.LojaCarro.dto.AgendamentoRequest;
import br.org.edu.ifrn.LojaCarro.dto.AgendamentoResponse;
import br.org.edu.ifrn.LojaCarro.exception.AcessoNegadoException;
import br.org.edu.ifrn.LojaCarro.exception.ConflitoHorarioException;
import br.org.edu.ifrn.LojaCarro.exception.RegraNegocioException;
import br.org.edu.ifrn.LojaCarro.model.*;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.repository.VeiculoRepository;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    // "Agora": segunda-feira, 04/03/2030, 09:00
    private static final LocalDateTime AGORA = LocalDateTime.of(2030, 3, 4, 9, 0);
    private static final LocalDate TERCA = LocalDate.of(2030, 3, 5);

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioLogado usuarioLogado;

    @Mock
    private LogService logService;

    private AgendamentoService agendamentoService;

    private Usuario ana;
    private Veiculo corolla;

    @BeforeEach
    void criarServico() {
        Clock relogio = Clock.fixed(AGORA.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        HorarioFuncionamento horario = new HorarioFuncionamento("08:00", "18:00", "12:00");
        agendamentoService = new AgendamentoService(agendamentoRepository, veiculoRepository, usuarioRepository,
                horario, usuarioLogado, logService, relogio);

        ana = new Usuario("Ana", "ana@email.com", "hash", Perfil.CLIENTE);
        ana.setId(1L);
        corolla = new Veiculo();
        corolla.setId(10L);
        corolla.setMarca("Toyota");
        corolla.setModelo("Corolla");
        corolla.setAnoFabricacao(2023);
        corolla.setAnoModelo(2024);
        corolla.setFotoPrincipal("abcdef12-foto.png");
        corolla.setSituacao(SituacaoVeiculo.DISPONIVEL);
    }

    private void anaLogada() {
        when(usuarioLogado.obrigatorio()).thenReturn(new UsuarioAutenticado(1L, "Ana", "ana@email.com", Perfil.CLIENTE));
    }

    private void veiculoEClienteBloqueados() {
        when(veiculoRepository.buscarComBloqueio(10L)).thenReturn(Optional.of(corolla));
        when(usuarioRepository.buscarComBloqueio(1L)).thenReturn(Optional.of(ana));
    }

    @Test
    void agendaTestDriveDe30MinutosQuandoOHorarioEstaLivre() {
        anaLogada();
        veiculoEClienteBloqueados();
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(inv -> inv.getArgument(0));

        AgendamentoResponse resposta = agendamentoService.agendar(new AgendamentoRequest(10L, TERCA, LocalTime.of(14, 0)));

        ArgumentCaptor<Agendamento> salvo = ArgumentCaptor.forClass(Agendamento.class);
        verify(agendamentoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getDataHoraInicio()).isEqualTo(LocalDateTime.of(TERCA, LocalTime.of(14, 0)));
        assertThat(salvo.getValue().getDataHoraFim()).isEqualTo(LocalDateTime.of(TERCA, LocalTime.of(14, 30)));
        assertThat(salvo.getValue().getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
        assertThat(resposta.horarioFim()).isEqualTo(LocalTime.of(14, 30));
        // A verificação de conflito acontece depois de bloquear o veículo
        var ordem = inOrder(veiculoRepository, agendamentoRepository);
        ordem.verify(veiculoRepository).buscarComBloqueio(10L);
        ordem.verify(agendamentoRepository).existeSobreposicaoNoVeiculo(eq(10L), eq(StatusAgendamento.CONFIRMADO), any(), any());
    }

    @Test
    void recusaHorarioJaOcupadoNoVeiculo() {
        anaLogada();
        veiculoEClienteBloqueados();
        LocalDateTime inicio = LocalDateTime.of(TERCA, LocalTime.of(14, 15));
        when(agendamentoRepository.existeSobreposicaoNoVeiculo(10L, StatusAgendamento.CONFIRMADO, inicio, inicio.plusMinutes(30)))
                .thenReturn(true);

        assertThatThrownBy(() -> agendamentoService.agendar(new AgendamentoRequest(10L, TERCA, LocalTime.of(14, 15))))
                .isInstanceOf(ConflitoHorarioException.class);
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void recusaDataPassadaSemConsultarOBanco() {
        anaLogada();

        assertThatThrownBy(() -> agendamentoService.agendar(
                new AgendamentoRequest(10L, LocalDate.of(2030, 3, 4), LocalTime.of(8, 30))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("passado");
        verifyNoInteractions(veiculoRepository, agendamentoRepository);
    }

    @Test
    void recusaHorarioQueTerminaDepoisDoFechamento() {
        anaLogada();

        assertThatThrownBy(() -> agendamentoService.agendar(new AgendamentoRequest(10L, TERCA, LocalTime.of(17, 45))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("fora do funcionamento");
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void recusaVeiculoVendido() {
        anaLogada();
        corolla.setSituacao(SituacaoVeiculo.VENDIDO);
        when(veiculoRepository.buscarComBloqueio(10L)).thenReturn(Optional.of(corolla));

        assertThatThrownBy(() -> agendamentoService.agendar(new AgendamentoRequest(10L, TERCA, LocalTime.of(10, 0))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não está disponível");
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    void cancelaAgendamentoFuturoDoProprioCliente() {
        anaLogada();
        Agendamento agendamento = new Agendamento(ana, corolla, LocalDateTime.of(TERCA, LocalTime.of(14, 0)));
        when(agendamentoRepository.findById(5L)).thenReturn(Optional.of(agendamento));

        AgendamentoResponse resposta = agendamentoService.cancelar(5L);

        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(agendamento.getDataCancelamento()).isEqualTo(AGORA);
        assertThat(resposta.cancelavel()).isFalse();
        verify(logService).registrar(eq("CANCELAR"), eq("Agendamento"), anyString());
    }

    @Test
    void clienteNaoCancelaAgendamentoDeOutroCliente() {
        Usuario bruno = new Usuario("Bruno", "bruno@email.com", "hash", Perfil.CLIENTE);
        bruno.setId(2L);
        Agendamento doBruno = new Agendamento(bruno, corolla, LocalDateTime.of(TERCA, LocalTime.of(14, 0)));
        anaLogada();
        when(agendamentoRepository.findById(7L)).thenReturn(Optional.of(doBruno));

        assertThatThrownBy(() -> agendamentoService.cancelar(7L)).isInstanceOf(AcessoNegadoException.class);
        assertThat(doBruno.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }

    @Test
    void naoCancelaAgendamentoQueJaComecou() {
        anaLogada();
        Agendamento deHoje = new Agendamento(ana, corolla, LocalDateTime.of(2030, 3, 4, 8, 30));
        when(agendamentoRepository.findById(8L)).thenReturn(Optional.of(deHoje));

        assertThatThrownBy(() -> agendamentoService.cancelar(8L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ainda não começaram");
        assertThat(deHoje.getStatus()).isEqualTo(StatusAgendamento.CONFIRMADO);
    }
}
