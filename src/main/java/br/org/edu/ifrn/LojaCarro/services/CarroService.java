package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.CarroException;
import br.org.edu.ifrn.LojaCarro.model.Carro;
import br.org.edu.ifrn.LojaCarro.repository.CarroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CarroService {

    private static final String ENTIDADE = "Carro";

    @Autowired
    public CarroRepository carroRepository;

    @Autowired
    private LogService logService;

    @Autowired
    private SessaoService sessaoService;

    public Carro save(Carro c) {
        validarModelo(c.getModelo());  // Valida o modelo antes de salvar
        validarPreco(c.getPreco());
        c.setCadastradoPor(sessaoService.usuarioLogado().orElse(null));
        Carro salvo = carroRepository.save(c);
        logService.registrar("CRIAR", ENTIDADE, "Carro criado: ID " + salvo.getId() + ", modelo " + salvo.getModelo());
        return salvo;
    }

    // Novo método para deletar por ID
    public void deleteById(Long id) {
        if(id <= 0){
            throw new CarroException("O ID do carro não pode ser negativo. ID fornecido: " + id);
        }
        carroRepository.deleteById(id);
        logService.registrar("EXCLUIR", ENTIDADE, "Carro excluído: ID " + id);
    }

    // Novo método para pesquisar por ID
    public Optional<Carro> findById(Long id) {
        if(id <= 0){
            throw new CarroException("O ID do carro não pode ser negativo. ID fornecido: " + id);
        }
        logService.registrar("CONSULTAR", ENTIDADE, "Consulta do carro ID " + id);
        return carroRepository.findById(id);
    }

    // Novo método para listar todos os carros
    public List<Carro> findAll() {
        logService.registrar("CONSULTAR", ENTIDADE, "Listagem de todos os carros");
        return carroRepository.findAll();
    }

    public Optional<Carro> findByModelo(String modelo) {
        validarModelo(modelo);
        logService.registrar("CONSULTAR", ENTIDADE, "Consulta do carro modelo " + modelo);
        return carroRepository.findFirstByModelo(modelo);
    }

    public Carro saveFromLegacy(String modelo, double preco) {
        Carro carro = new Carro(modelo, LocalDate.now().getYear(), preco);
        return save(carro);
    }

    public Carro updateByModelo(String modelo, double preco) {
        Carro carro = localizarCarroPorModelo(modelo);
        validarPreco(preco);
        carro.setPreco(preco);
        Carro atualizado = carroRepository.save(carro);
        logService.registrar("ATUALIZAR", ENTIDADE, "Preço do carro modelo " + modelo + " atualizado para " + preco);
        return atualizado;
    }

    public Carro deleteByModelo(String modelo) {
        Carro carro = localizarCarroPorModelo(modelo);
        carroRepository.delete(carro);
        logService.registrar("EXCLUIR", ENTIDADE, "Carro excluído: ID " + carro.getId() + ", modelo " + modelo);
        return carro;
    }

    // Método para atualizar (usa o save existente, mas pode ser renomeado se preferir)
    public Carro update(Carro c) {
        if (c.getId() == null) {
            throw new CarroException("O ID do carro para atualização não pode ser nulo.");
        }
        Carro existente = carroRepository.findById(c.getId())
                .orElseThrow(() -> new CarroException("Carro com ID " + c.getId() + " não encontrado para atualização."));
        validarModelo(c.getModelo());  // Valida o modelo antes de atualizar
        validarPreco(c.getPreco());
        c.setCadastradoPor(existente.getCadastradoPor());  // Mantém quem cadastrou o carro
        Carro atualizado = carroRepository.save(c);  // Retorna o carro salvo para feedback
        logService.registrar("ATUALIZAR", ENTIDADE, "Carro atualizado: ID " + atualizado.getId() + ", modelo " + atualizado.getModelo());
        return atualizado;
    }

    // Validação do modelo
    private void validarModelo(String modelo) {
        if (modelo == null || modelo.trim().isEmpty()) {
            throw new CarroException("O modelo do carro não pode estar vazio.");
        }
        if (modelo.length() >= 5) {
            throw new CarroException("O modelo do carro deve ter menos de 5 caracteres. Tamanho atual: " + modelo.length());
        }
    }

    private void validarPreco(double preco) {
        if (preco < 0) {
            throw new CarroException("O preço do carro não pode ser negativo. Valor fornecido: " + preco);
        }
    }

    private Carro localizarCarroPorModelo(String modelo) {
        validarModelo(modelo);
        return carroRepository.findFirstByModelo(modelo)
                .orElseThrow(() -> new CarroException("Carro com modelo " + modelo + " não encontrado."));
    }
}
