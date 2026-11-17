package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.VeiculoFiltro;
import br.org.edu.ifrn.LojaCarro.dto.VeiculoRequest;
import br.org.edu.ifrn.LojaCarro.dto.VeiculoResponse;
import br.org.edu.ifrn.LojaCarro.exception.RecursoNaoEncontradoException;
import br.org.edu.ifrn.LojaCarro.exception.RegraNegocioException;
import br.org.edu.ifrn.LojaCarro.model.SituacaoVeiculo;
import br.org.edu.ifrn.LojaCarro.model.Veiculo;
import br.org.edu.ifrn.LojaCarro.repository.UsuarioRepository;
import br.org.edu.ifrn.LojaCarro.repository.VeiculoRepository;
import br.org.edu.ifrn.LojaCarro.repository.VeiculoSpecifications;
import br.org.edu.ifrn.LojaCarro.security.UsuarioLogado;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Year;
import java.util.List;

@Service
public class VeiculoService {

    private static final String ENTIDADE = "Veiculo";

    private final VeiculoRepository veiculoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ImagemService imagemService;
    private final LogService logService;
    private final UsuarioLogado usuarioLogado;

    public VeiculoService(VeiculoRepository veiculoRepository, UsuarioRepository usuarioRepository,
                          ImagemService imagemService, LogService logService, UsuarioLogado usuarioLogado) {
        this.veiculoRepository = veiculoRepository;
        this.usuarioRepository = usuarioRepository;
        this.imagemService = imagemService;
        this.logService = logService;
        this.usuarioLogado = usuarioLogado;
    }

    public record FotoVeiculo(Resource arquivo, MediaType tipo) {
    }

    // Visitantes e clientes veem só os veículos disponíveis. O administrador pode filtrar por qualquer situação.
    @Transactional(readOnly = true)
    public List<VeiculoResponse> buscar(VeiculoFiltro filtro) {
        validarFiltro(filtro);
        boolean filtrarSituacao = usuarioLogado.isAdmin() && filtro.situacoes() != null && !filtro.situacoes().isEmpty();
        VeiculoFiltro filtroFinal = filtrarSituacao ? filtro : filtro.comSituacoes(List.of(SituacaoVeiculo.DISPONIVEL));

        return veiculoRepository.findAll(VeiculoSpecifications.comFiltro(filtroFinal), Sort.by("marca", "modelo", "id"))
                .stream()
                .map(VeiculoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public VeiculoResponse buscarPorId(Long id) {
        return VeiculoResponse.de(buscarVisivel(id));
    }

    @Transactional(readOnly = true)
    public FotoVeiculo buscarFoto(Long id) {
        Veiculo veiculo = buscarVisivel(id);
        if (veiculo.getFotoPrincipal() == null) {
            throw new RecursoNaoEncontradoException("O veículo com ID " + id + " não possui fotografia.");
        }
        return new FotoVeiculo(imagemService.carregar(veiculo.getFotoPrincipal()),
                imagemService.tipoDe(veiculo.getFotoPrincipal()));
    }

    // Todo veículo é cadastrado com a fotografia principal
    @Transactional
    public VeiculoResponse cadastrar(VeiculoRequest dados, MultipartFile foto) {
        validarRegras(dados);
        Veiculo veiculo = new Veiculo();
        copiarDados(dados, veiculo);
        veiculo.setSituacao(SituacaoVeiculo.DISPONIVEL);
        veiculo.setCadastradoPor(usuarioRepository.getReferenceById(usuarioLogado.obrigatorio().id()));

        String arquivo = imagemService.salvar(foto);
        aoFinalizarTransacao(() -> { }, () -> imagemService.excluir(arquivo));
        veiculo.setFotoPrincipal(arquivo);

        Veiculo salvo = veiculoRepository.save(veiculo);
        logService.registrar("CRIAR", ENTIDADE, "Veículo cadastrado: ID " + salvo.getId() + ", " + descrever(salvo)
                + ", foto " + arquivo);
        return VeiculoResponse.de(salvo);
    }

    @Transactional
    public VeiculoResponse trocarFoto(Long id, MultipartFile foto) {
        Veiculo veiculo = buscarEntidade(id);
        String anterior = veiculo.getFotoPrincipal();

        String nova = imagemService.salvar(foto);
        aoFinalizarTransacao(() -> imagemService.excluir(anterior), () -> imagemService.excluir(nova));
        veiculo.setFotoPrincipal(nova);

        Veiculo salvo = veiculoRepository.save(veiculo);
        logService.registrar("ATUALIZAR_FOTO", ENTIDADE, "Foto do veículo ID " + id + " (" + descrever(salvo)
                + ") substituída: " + anterior + " -> " + nova);
        return VeiculoResponse.de(salvo);
    }

    @Transactional
    public VeiculoResponse atualizar(Long id, VeiculoRequest dados) {
        Veiculo veiculo = buscarEntidade(id);
        validarRegras(dados);
        copiarDados(dados, veiculo);

        Veiculo salvo = veiculoRepository.save(veiculo);
        logService.registrar("ATUALIZAR", ENTIDADE, "Veículo atualizado: ID " + id + ", " + descrever(salvo));
        return VeiculoResponse.de(salvo);
    }

    // Marca o veículo como disponível, vendido ou inativo
    @Transactional
    public VeiculoResponse alterarSituacao(Long id, SituacaoVeiculo novaSituacao) {
        Veiculo veiculo = buscarEntidade(id);
        SituacaoVeiculo anterior = veiculo.getSituacao();
        veiculo.setSituacao(novaSituacao);

        Veiculo salvo = veiculoRepository.save(veiculo);
        logService.registrar("ALTERAR_SITUACAO", ENTIDADE,
                "Veículo ID " + id + " (" + descrever(salvo) + "): " + anterior + " -> " + novaSituacao);
        return VeiculoResponse.de(salvo);
    }

    // Exclusão lógica: o registro continua no banco para preservar o histórico de agendamentos
    @Transactional
    public void desativar(Long id) {
        alterarSituacao(id, SituacaoVeiculo.INATIVO);
    }

    // Veículo inativo (excluído logicamente) só é visível para o administrador
    private Veiculo buscarVisivel(Long id) {
        Veiculo veiculo = buscarEntidade(id);
        if (veiculo.getSituacao() == SituacaoVeiculo.INATIVO && !usuarioLogado.isAdmin()) {
            throw new RecursoNaoEncontradoException("Veículo com ID " + id + " não encontrado.");
        }
        return veiculo;
    }

    // O arquivo de imagem não participa da transação do banco: se ela for desfeita, a imagem nova é apagada;
    // se for confirmada, a imagem antiga (quando houver) é que é apagada.
    private void aoFinalizarTransacao(Runnable seConfirmada, Runnable seDesfeita) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            seConfirmada.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    seConfirmada.run();
                } else {
                    seDesfeita.run();
                }
            }
        });
    }

    private Veiculo buscarEntidade(Long id) {
        return veiculoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Veículo com ID " + id + " não encontrado."));
    }

    // Regras que dependem de mais de um campo (as validações simples ficam nas anotações do VeiculoRequest)
    private void validarRegras(VeiculoRequest dados) {
        int anoAtual = Year.now().getValue();
        if (dados.anoFabricacao() > anoAtual) {
            throw new RegraNegocioException("O ano de fabricação não pode ser maior que o ano atual (" + anoAtual + ").");
        }
        if (dados.anoModelo() < dados.anoFabricacao() || dados.anoModelo() > dados.anoFabricacao() + 1) {
            throw new RegraNegocioException("O ano do modelo deve ser igual ao ano de fabricação ou o ano seguinte.");
        }
    }

    private void validarFiltro(VeiculoFiltro filtro) {
        if (filtro.precoMin() != null && filtro.precoMax() != null && filtro.precoMin().compareTo(filtro.precoMax()) > 0) {
            throw new RegraNegocioException("O preço mínimo não pode ser maior que o preço máximo.");
        }
        if (filtro.anoMin() != null && filtro.anoMax() != null && filtro.anoMin() > filtro.anoMax()) {
            throw new RegraNegocioException("O ano mínimo não pode ser maior que o ano máximo.");
        }
    }

    private void copiarDados(VeiculoRequest dados, Veiculo veiculo) {
        veiculo.setMarca(dados.marca().trim());
        veiculo.setModelo(dados.modelo().trim());
        veiculo.setAnoFabricacao(dados.anoFabricacao());
        veiculo.setAnoModelo(dados.anoModelo());
        veiculo.setCor(dados.cor().trim());
        veiculo.setQuilometragem(dados.quilometragem());
        veiculo.setCombustivel(dados.combustivel());
        veiculo.setTransmissao(dados.transmissao());
        veiculo.setPreco(dados.preco());
        veiculo.setDescricao(dados.descricao() == null || dados.descricao().isBlank() ? null : dados.descricao().trim());
    }

    private String descrever(Veiculo v) {
        return v.getMarca() + " " + v.getModelo() + " " + v.getAnoFabricacao() + "/" + v.getAnoModelo();
    }
}
