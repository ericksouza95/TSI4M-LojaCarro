package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.dto.VeiculoFiltro;
import br.org.edu.ifrn.LojaCarro.dto.VeiculoRequest;
import br.org.edu.ifrn.LojaCarro.dto.VeiculoResponse;
import br.org.edu.ifrn.LojaCarro.exception.RecursoNaoEncontradoException;
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
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private LogService logService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioLogado usuarioLogado;

    @Mock
    private ImagemService imagemService;

    @BeforeEach
    void criarServico() {
        Clock relogio = Clock.fixed(Instant.parse("2030-03-04T12:00:00Z"), ZoneId.of("America/Fortaleza"));
        veiculoService = new VeiculoService(veiculoRepository, usuarioRepository, agendamentoRepository,
                imagemService, logService, usuarioLogado, relogio);
    }

    private final MockMultipartFile foto = new MockMultipartFile("foto", "carro.png", "image/png", new byte[]{1, 2, 3});

    @Mock
    private AgendamentoRepository agendamentoRepository;

    private VeiculoService veiculoService;

    private static VeiculoRequest request(int anoFabricacao, int anoModelo, String preco) {
        return new VeiculoRequest("Toyota", " Corolla ", anoFabricacao, anoModelo, "Prata", 30000,
                Combustivel.FLEX, Transmissao.AUTOMATICA, new BigDecimal(preco), "  ");
    }

    private static Veiculo veiculoExistente(Long id) {
        Veiculo v = new Veiculo();
        v.setId(id);
        v.setMarca("Toyota");
        v.setModelo("Corolla");
        v.setAnoFabricacao(2020);
        v.setAnoModelo(2020);
        v.setSituacao(SituacaoVeiculo.DISPONIVEL);
        return v;
    }

    @Test
    void cadastraVeiculoDisponivelVinculadoAoUsuarioLogado() {
        Usuario admin = new Usuario("Admin", "admin@email.com", "hash", Perfil.ADMIN);
        when(usuarioLogado.obrigatorio()).thenReturn(new UsuarioAutenticado(1L, "Admin", "admin@email.com", Perfil.ADMIN));
        when(usuarioRepository.getReferenceById(1L)).thenReturn(admin);
        when(imagemService.salvar(foto)).thenReturn("1234abcd-0000-0000-0000-000000000000.png");
        when(veiculoRepository.save(any(Veiculo.class))).thenAnswer(inv -> {
            Veiculo v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        VeiculoResponse resposta = veiculoService.cadastrar(request(2022, 2023, "120000"), foto);

        ArgumentCaptor<Veiculo> salvo = ArgumentCaptor.forClass(Veiculo.class);
        verify(veiculoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getCadastradoPor()).isSameAs(admin);
        assertThat(salvo.getValue().getModelo()).isEqualTo("Corolla");   // texto aparado
        assertThat(salvo.getValue().getDescricao()).isNull();           // descrição em branco vira nula
        assertThat(salvo.getValue().getFotoPrincipal()).isEqualTo("1234abcd-0000-0000-0000-000000000000.png");
        assertThat(resposta.fotoUrl()).isEqualTo("/api/veiculos/1/foto?v=1234abcd");
        assertThat(resposta.situacao()).isEqualTo(SituacaoVeiculo.DISPONIVEL);
        verify(logService).registrar(eq("CRIAR"), eq("Veiculo"), contains("Toyota Corolla"));
    }

    @Test
    void naoCadastraComAnoDoModeloAnteriorAoDeFabricacao() {
        assertThatThrownBy(() -> veiculoService.cadastrar(request(2022, 2021, "120000"), foto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ano do modelo");
        verify(veiculoRepository, never()).save(any());
        verify(imagemService, never()).salvar(any());
    }

    @Test
    void naoCadastraComAnoDoModeloDoisAnosAFrente() {
        assertThatThrownBy(() -> veiculoService.cadastrar(request(2020, 2022, "120000"), foto))
                .isInstanceOf(RegraNegocioException.class);
        verify(veiculoRepository, never()).save(any());
    }

    @Test
    void naoCadastraComAnoDeFabricacaoNoFuturo() {
        int anoQueVem = Year.now().getValue() + 1;

        assertThatThrownBy(() -> veiculoService.cadastrar(request(anoQueVem, anoQueVem, "120000"), foto))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ano de fabricação");
        verify(veiculoRepository, never()).save(any());
    }

    @Test
    void atualizaOsDadosDoVeiculoExistente() {
        Veiculo existente = veiculoExistente(5L);
        when(veiculoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(veiculoRepository.save(existente)).thenReturn(existente);

        VeiculoResponse resposta = veiculoService.atualizar(5L, request(2021, 2022, "99900.50"));

        assertThat(resposta.anoFabricacao()).isEqualTo(2021);
        assertThat(resposta.anoModelo()).isEqualTo(2022);
        assertThat(resposta.preco()).isEqualByComparingTo("99900.50");
        verify(logService).registrar(eq("ATUALIZAR"), eq("Veiculo"), anyString());
    }

    @Test
    void atualizarVeiculoInexistenteLancaNaoEncontrado() {
        when(veiculoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> veiculoService.atualizar(99L, request(2021, 2021, "1000")))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(veiculoRepository, never()).save(any());
    }

    @Test
    void desativarFazExclusaoLogica() {
        Veiculo existente = veiculoExistente(7L);
        when(veiculoRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(veiculoRepository.save(existente)).thenReturn(existente);

        veiculoService.desativar(7L);

        assertThat(existente.getSituacao()).isEqualTo(SituacaoVeiculo.INATIVO);
        verify(veiculoRepository, never()).delete(any(Veiculo.class));
        verify(veiculoRepository, never()).deleteById(any());
    }

    @Test
    void trocarFotoGravaANovaEApagaAAnterior() {
        Veiculo existente = veiculoExistente(6L);
        existente.setFotoPrincipal("antiga.png");
        when(veiculoRepository.findById(6L)).thenReturn(Optional.of(existente));
        when(imagemService.salvar(foto)).thenReturn("nova0000-0000-0000-0000-000000000000.webp");
        when(veiculoRepository.save(existente)).thenReturn(existente);

        veiculoService.trocarFoto(6L, foto);

        assertThat(existente.getFotoPrincipal()).isEqualTo("nova0000-0000-0000-0000-000000000000.webp");
        verify(imagemService).excluir("antiga.png");
        verify(logService).registrar(eq("ATUALIZAR_FOTO"), eq("Veiculo"), contains("antiga.png"));
    }

    @Test
    void veiculoInativoNaoEhEncontradoPorQuemNaoEhAdministrador() {
        Veiculo inativo = veiculoExistente(8L);
        inativo.setSituacao(SituacaoVeiculo.INATIVO);
        when(veiculoRepository.findById(8L)).thenReturn(Optional.of(inativo));
        when(usuarioLogado.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> veiculoService.buscarPorId(8L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void administradorConsultaVeiculoInativo() {
        Veiculo inativo = veiculoExistente(8L);
        inativo.setSituacao(SituacaoVeiculo.INATIVO);
        when(veiculoRepository.findById(8L)).thenReturn(Optional.of(inativo));
        when(usuarioLogado.isAdmin()).thenReturn(true);

        assertThat(veiculoService.buscarPorId(8L).situacao()).isEqualTo(SituacaoVeiculo.INATIVO);
    }

    @Test
    void filtroComFaixaDePrecoInvertidaEhRejeitado() {
        VeiculoFiltro filtro = new VeiculoFiltro(null, new BigDecimal("50000"), new BigDecimal("10000"),
                null, null, List.of(SituacaoVeiculo.DISPONIVEL));

        assertThatThrownBy(() -> veiculoService.buscar(filtro))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("preço mínimo");
        verifyNoInteractions(veiculoRepository);
    }
}
