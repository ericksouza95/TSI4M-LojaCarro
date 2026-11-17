package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import br.org.edu.ifrn.LojaCarro.model.StatusAgendamento;
import br.org.edu.ifrn.LojaCarro.model.Usuario;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>, JpaSpecificationExecutor<Agendamento> {

    boolean existsByCliente(Usuario cliente);

    // Dois intervalos [inicio, fim) se sobrepõem quando um começa antes do outro terminar e termina depois do outro começar.
    // Ex.: existente 14:00-14:30 x novo 14:15-14:45 -> conflito; x novo 14:30-15:00 -> sem conflito.
    @Query("""
            select count(a) > 0 from Agendamento a
            where a.veiculo.id = :veiculoId
              and a.status = :status
              and a.dataHoraInicio < :fim
              and a.dataHoraFim > :inicio
            """)
    boolean existeSobreposicaoNoVeiculo(Long veiculoId, StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim);

    @Query("""
            select count(a) > 0 from Agendamento a
            where a.cliente.id = :clienteId
              and a.status = :status
              and a.dataHoraInicio < :fim
              and a.dataHoraFim > :inicio
            """)
    boolean existeSobreposicaoDoCliente(Long clienteId, StatusAgendamento status, LocalDateTime inicio, LocalDateTime fim);

    // Agendamentos de um veículo num período (usado para montar a grade de horários do dia)
    List<Agendamento> findByVeiculoIdAndStatusAndDataHoraInicioBetween(Long veiculoId, StatusAgendamento status,
                                                                       LocalDateTime de, LocalDateTime ate);

    // Agendamentos futuros de um veículo (cancelados quando o veículo é vendido ou desativado)
    List<Agendamento> findByVeiculoIdAndStatusAndDataHoraInicioAfter(Long veiculoId, StatusAgendamento status,
                                                                     LocalDateTime depoisDe);

    @EntityGraph(attributePaths = {"veiculo", "cliente"})
    List<Agendamento> findByClienteIdOrderByDataHoraInicioDesc(Long clienteId);

    @Override
    @EntityGraph(attributePaths = {"veiculo", "cliente"})
    List<Agendamento> findAll(Specification<Agendamento> spec, Sort sort);
}
