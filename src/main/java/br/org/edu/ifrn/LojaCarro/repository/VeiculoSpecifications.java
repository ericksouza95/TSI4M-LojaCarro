package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.dto.VeiculoFiltro;
import br.org.edu.ifrn.LojaCarro.model.Veiculo;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

// Monta a consulta de veículos a partir dos filtros opcionais do catálogo
public final class VeiculoSpecifications {

    private VeiculoSpecifications() {
    }

    public static Specification<Veiculo> comFiltro(VeiculoFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();

            if (filtro.temBusca()) {
                String termo = "%" + filtro.busca().trim().toLowerCase() + "%";
                condicoes.add(cb.or(
                        cb.like(cb.lower(root.get("marca")), termo),
                        cb.like(cb.lower(root.get("modelo")), termo)));
            }
            if (filtro.precoMin() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.get("preco"), filtro.precoMin()));
            }
            if (filtro.precoMax() != null) {
                condicoes.add(cb.lessThanOrEqualTo(root.get("preco"), filtro.precoMax()));
            }
            if (filtro.anoMin() != null) {
                condicoes.add(cb.greaterThanOrEqualTo(root.get("anoModelo"), filtro.anoMin()));
            }
            if (filtro.anoMax() != null) {
                condicoes.add(cb.lessThanOrEqualTo(root.get("anoModelo"), filtro.anoMax()));
            }
            if (filtro.situacoes() != null && !filtro.situacoes().isEmpty()) {
                condicoes.add(root.get("situacao").in(filtro.situacoes()));
            }
            return cb.and(condicoes.toArray(new Predicate[0]));
        };
    }
}
