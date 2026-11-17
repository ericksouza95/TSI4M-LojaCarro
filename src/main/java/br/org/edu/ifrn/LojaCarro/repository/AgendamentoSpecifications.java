package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.dto.AgendamentoFiltro;
import br.org.edu.ifrn.LojaCarro.model.Agendamento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// Filtros opcionais da tela de gerenciamento de agendamentos (data, veículo e situação)
public final class AgendamentoSpecifications {

    private AgendamentoSpecifications() {
    }

    public static Specification<Agendamento> comFiltro(AgendamentoFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (filtro.data() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.get("dataHoraInicio"), filtro.data().atStartOfDay()));
                condicoes.add(cb.lessThan(root.get("dataHoraInicio"), filtro.data().plusDays(1).atStartOfDay()));
            }
            if (filtro.veiculoId() != null) {
                condicoes.add(cb.equal(root.get("veiculo").get("id"), filtro.veiculoId()));
            }
            if (filtro.status() != null) {
                condicoes.add(cb.equal(root.get("status"), filtro.status()));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
    }
}
