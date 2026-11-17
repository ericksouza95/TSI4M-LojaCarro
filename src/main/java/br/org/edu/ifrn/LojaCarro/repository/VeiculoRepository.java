package br.org.edu.ifrn.LojaCarro.repository;

import br.org.edu.ifrn.LojaCarro.model.Usuario;
import br.org.edu.ifrn.LojaCarro.model.Veiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VeiculoRepository extends JpaRepository<Veiculo, Long>, JpaSpecificationExecutor<Veiculo> {

    boolean existsByCadastradoPor(Usuario usuario);

    // SELECT ... FOR UPDATE: enquanto uma transação de agendamento estiver com o veículo,
    // outra que tente agendar o mesmo veículo espera ela terminar
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veiculo v where v.id = :id")
    Optional<Veiculo> buscarComBloqueio(Long id);
}
