package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.config.HorarioFuncionamento;
import br.org.edu.ifrn.LojaCarro.dto.AgendamentoFiltro;
import br.org.edu.ifrn.LojaCarro.dto.AgendamentoRequest;
import br.org.edu.ifrn.LojaCarro.dto.AgendamentoResponse;
import br.org.edu.ifrn.LojaCarro.dto.HorarioDisponivelResponse;
import br.org.edu.ifrn.LojaCarro.exception.AcessoNegadoException;
import br.org.edu.ifrn.LojaCarro.exception.AutenticacaoException;
import br.org.edu.ifrn.LojaCarro.exception.ConflitoHorarioException;
import br.org.edu.ifrn.LojaCarro.exception.RecursoNaoEncontradoException;
import br.org.edu.ifrn.LojaCarro.exception.RegraNegocioException;
import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.model.Veiculo;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoRepository;
import br.org.edu.ifrn.LojaCarro.repository.AgendamentoSpecifications;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.repository.VeiculoRepository;
import br.org.edu.ifrn.LojaCarro.security.UsuarioAutenticado;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static br.org.edu.ifrn.LojaCarro.config.HorarioFuncionamento.DURACAO_TEST_DRIVE_MINUTOS;
import static br.org.edu.ifrn.LojaCarro.model.StatusAgendamento.CONFIRMADO;

@Service
public class AgendamentoService {

    private static final String ENTIDADE = "Agendamento";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AgendamentoRepository agendamentoRepository;
    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HorarioFuncionamento horarioFuncionamento;
    private final UsuarioLogado usuarioLogado;
    private final LogService logService;
    private final Clock relogio;

    public AgendamentoService(AgendamentoRepository agendamentoRepository, VeiculoRepository veiculoRepository,
                              UsuarioRepository usuarioRepository, HorarioFuncionamento horarioFuncionamento,
                              UsuarioLogado usuarioLogado, LogService logService, Clock relogio) {
        this.agendamentoRepository = agendamentoRepository;
        this.veiculoRepository = veiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.horarioFuncionamento = horarioFuncionamento;
        this.usuarioLogado = usuarioLogado;
        this.logService = logService;
        this.relogio = relogio;
    }

    // Regra principal do RF04: um veículo não pode ter dois agendamentos CONFIRMADOS com horários sobrepostos.
    //
    // Concorrência: a linha do veículo é bloqueada (SELECT ... FOR UPDATE) ANTES de verificar o conflito.
    // Se dois clientes pedirem o mesmo horário ao mesmo tempo, a segunda transação espera a primeira terminar
    // e, como o isolamento é READ_COMMITTED, ela enxerga o agendamento que acabou de ser gravado e é recusada.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AgendamentoResponse agendar(AgendamentoRequest dados) {
        UsuarioAutenticado logado = usuarioLogado.obrigatorio();
        LocalDateTime inicio = LocalDateTime.of(dados.data(), dados.horario().withSecond(0).withNano(0));
        LocalDateTime fim = inicio.plusMinutes(DURACAO_TEST_DRIVE_MINUTOS);
        validarDataEHorario(inicio, fim);

        Veiculo veiculo = veiculoRepository.buscarComBloqueio(dados.veiculoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo com ID " + dados.veiculoId() + " não encontrado."));
        if (!veiculo.isDisponivel()) {
            throw new RegraNegocioException("Este veículo não está disponível para test drive.");
        }
        Usuario cliente = usuarioRepository.buscarComBloqueio(logado.id())
                .orElseThrow(() -> new AutenticacaoException("Usuário não encontrado."));

        if (agendamentoRepository.existeSobreposicaoNoVeiculo(veiculo.getId(), CONFIRMADO, inicio, fim)) {
            throw new ConflitoHorarioException("Este veículo já possui um test drive agendado que coincide com "
                    + inicio.format(FORMATO) + ". Escolha outro horário.");
        }
        if (agendamentoRepository.existeSobreposicaoDoCliente(cliente.getId(), CONFIRMADO, inicio, fim)) {
            throw new ConflitoHorarioException("Você já possui outro test drive agendado que coincide com "
                    + inicio.format(FORMATO) + ".");
        }

        Agendamento salvo = agendamentoRepository.save(new Agendamento(cliente, veiculo, inicio));
        logService.registrar("CRIAR", ENTIDADE, "Test drive agendado: ID " + salvo.getId() + ", veículo ID "
                + veiculo.getId() + " (" + veiculo.getMarca() + " " + veiculo.getModelo() + "), " + inicio.format(FORMATO));
        return AgendamentoResponse.de(salvo, agora());
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> meusAgendamentos() {
        Long clienteId = usuarioLogado.obrigatorio().id();
        LocalDateTime agora = agora();
        return agendamentoRepository.findByClienteIdOrderByDataHoraInicioDesc(clienteId).stream()
                .map(a -> AgendamentoResponse.de(a, agora))
                .toList();
    }

    // Consulta do administrador, com filtros opcionais por data, veículo e situação
    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listar(AgendamentoFiltro filtro) {
        LocalDateTime agora = agora();
        return agendamentoRepository.findAll(AgendamentoSpecifications.comFiltro(filtro), Sort.by("dataHoraInicio", "id"))
                .stream()
                .map(a -> AgendamentoResponse.de(a, agora))
                .toList();
    }

    @Transactional(readOnly = true)
    public AgendamentoResponse buscarPorId(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        verificarDonoOuAdmin(agendamento);
        return AgendamentoResponse.de(agendamento, agora());
    }

    // O cliente cancela os próprios agendamentos futuros; o administrador cancela qualquer um.
    // Depois do cancelamento o horário volta a ficar livre (a verificação só considera CONFIRMADOS).
    @Transactional
    public AgendamentoResponse cancelar(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        verificarDonoOuAdmin(agendamento);
        LocalDateTime agora = agora();
        if (!agendamento.isConfirmado()) {
            throw new RegraNegocioException("Só agendamentos confirmados podem ser cancelados. Situação atual: "
                    + agendamento.getStatus() + ".");
        }
        if (!agendamento.podeSerCancelado(agora)) {
            throw new RegraNegocioException("Só é possível cancelar agendamentos que ainda não começaram.");
        }
        agendamento.cancelar(agora);
        logService.registrar("CANCELAR", ENTIDADE, "Test drive cancelado: ID " + id + ", "
                + agendamento.getDataHoraInicio().format(FORMATO));
        return AgendamentoResponse.de(agendamento, agora);
    }

    // O administrador registra que o test drive aconteceu
    @Transactional
    public AgendamentoResponse marcarComoRealizado(Long id) {
        Agendamento agendamento = buscarEntidade(id);
        LocalDateTime agora = agora();
        if (!agendamento.isConfirmado()) {
            throw new RegraNegocioException("Só agendamentos confirmados podem ser marcados como realizados.");
        }
        if (agendamento.getDataHoraInicio().isAfter(agora)) {
            throw new RegraNegocioException("O test drive ainda não aconteceu.");
        }
        agendamento.marcarComoRealizado();
        logService.registrar("REALIZAR", ENTIDADE, "Test drive realizado: ID " + id);
        return AgendamentoResponse.de(agendamento, agora);
    }

    // Grade do dia para o cliente escolher: cada horário de 30 min com indicação se está livre
    @Transactional(readOnly = true)
    public List<HorarioDisponivelResponse> horariosDisponiveis(Long veiculoId, LocalDate data) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo com ID " + veiculoId + " não encontrado."));
        if (!veiculo.isDisponivel()) {
            throw new RegraNegocioException("Este veículo não está disponível para test drive.");
        }
        List<Agendamento> ocupados = agendamentoRepository.findByVeiculoIdAndStatusAndDataHoraInicioBetween(
                veiculoId, CONFIRMADO, data.atStartOfDay(), data.plusDays(1).atStartOfDay());
        LocalDateTime agora = agora();

        return horarioFuncionamento.horariosDoDia(data).stream()
                .map(horario -> {
                    LocalDateTime inicio = LocalDateTime.of(data, horario);
                    LocalDateTime fim = inicio.plusMinutes(DURACAO_TEST_DRIVE_MINUTOS);
                    boolean livre = inicio.isAfter(agora) && ocupados.stream().noneMatch(a ->
                            a.getDataHoraInicio().isBefore(fim) && a.getDataHoraFim().isAfter(inicio));
                    return new HorarioDisponivelResponse(horario, fim.toLocalTime(), livre);
                })
                .toList();
    }

    private void validarDataEHorario(LocalDateTime inicio, LocalDateTime fim) {
        if (!inicio.isAfter(agora())) {
            throw new RegraNegocioException("Não é possível agendar um test drive em data ou horário passado.");
        }
        if (!horarioFuncionamento.permite(inicio, fim)) {
            throw new RegraNegocioException("Horário fora do funcionamento da loja ("
                    + horarioFuncionamento.descricao() + "). O test drive dura "
                    + DURACAO_TEST_DRIVE_MINUTOS + " minutos e precisa terminar até o fechamento.");
        }
    }

    // Impede que um cliente veja ou cancele agendamentos de outro cliente
    private void verificarDonoOuAdmin(Agendamento agendamento) {
        UsuarioAutenticado logado = usuarioLogado.obrigatorio();
        if (!logado.isAdmin() && !agendamento.getCliente().getId().equals(logado.id())) {
            throw new AcessoNegadoException("Você só pode acessar os seus próprios agendamentos.");
        }
    }

    private Agendamento buscarEntidade(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento com ID " + id + " não encontrado."));
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio);
    }
}
