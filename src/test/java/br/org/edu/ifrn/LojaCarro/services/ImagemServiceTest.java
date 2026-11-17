package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.exception.ArquivoInvalidoException;
import br.org.edu.ifrn.LojaCarro.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 0x4A, 0x46, 0x49, 0x46};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final byte[] WEBP = {0x52, 0x49, 0x46, 0x46, 0x24, 0, 0, 0, 0x57, 0x45, 0x42, 0x50, 0x56, 0x50};

    @TempDir
    Path pasta;

    private ImagemService imagemService;

    @BeforeEach
    void criarServico() {
        imagemService = new ImagemService(pasta.toString());
    }

    private static MockMultipartFile arquivo(String nome, byte[] conteudo) {
        return new MockMultipartFile("foto", nome, "image/qualquer", conteudo);
    }

    @Test
    void aceitaJpegPngEWebpPeloConteudo() throws Exception {
        String jpg = imagemService.salvar(arquivo("a.jpeg", JPEG));
        String png = imagemService.salvar(arquivo("b.PNG", PNG));
        String webp = imagemService.salvar(arquivo("c.webp", WEBP));

        assertThat(jpg).endsWith(".jpg");
        assertThat(png).endsWith(".png");
        assertThat(webp).endsWith(".webp");
        assertThat(Files.readAllBytes(pasta.resolve(png))).isEqualTo(PNG);
        assertThat(imagemService.tipoDe(webp).toString()).isEqualTo("image/webp");
        assertThat(imagemService.tipoDe(jpg)).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    void recusaArquivoComExtensaoDeImagemMasConteudoDeOutroTipo() {
        assertThatThrownBy(() -> imagemService.salvar(arquivo("virus.jpg", "MZ executavel".getBytes())))
                .isInstanceOf(ArquivoInvalidoException.class)
                .hasMessageContaining("não é uma imagem");
        assertThat(pasta).isEmptyDirectory();
    }

    @Test
    void recusaExtensaoNaoPermitidaMesmoComConteudoDeImagem() {
        assertThatThrownBy(() -> imagemService.salvar(arquivo("imagem.gif", PNG)))
                .isInstanceOf(ArquivoInvalidoException.class)
                .hasMessageContaining("Formato não permitido");
    }

    @Test
    void recusaArquivoAcimaDe5Mb() {
        byte[] grande = new byte[(int) ImagemService.TAMANHO_MAXIMO_BYTES + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        assertThatThrownBy(() -> imagemService.salvar(arquivo("grande.png", grande)))
                .isInstanceOf(ArquivoInvalidoException.class)
                .satisfies(e -> assertThat(((ArquivoInvalidoException) e).getStatus()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    void recusaArquivoVazioOuAusente() {
        assertThatThrownBy(() -> imagemService.salvar(null)).isInstanceOf(ArquivoInvalidoException.class);
        assertThatThrownBy(() -> imagemService.salvar(arquivo("vazio.png", new byte[0])))
                .isInstanceOf(ArquivoInvalidoException.class)
                .hasMessage("A fotografia do veículo é obrigatória.");
    }

    @Test
    void nomeDoArquivoEnviadoNaoEhUsadoNoDisco() {
        String nome = imagemService.salvar(arquivo("../../fora-da-pasta.png", PNG));

        assertThat(nome).matches("[0-9a-f-]{36}\\.png");
        assertThat(pasta.resolve(nome)).exists();
        assertThat(pasta.getParent().resolve("fora-da-pasta.png")).doesNotExist();
    }

    @Test
    void naoLeArquivosForaDaPastaDeImagens() {
        assertThatThrownBy(() -> imagemService.carregar("../segredo.txt"))
                .isInstanceOf(ArquivoInvalidoException.class);
        assertThatThrownBy(() -> imagemService.carregar("inexistente.png"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void excluiImagem() {
        String nome = imagemService.salvar(arquivo("a.png", PNG));

        imagemService.excluir(nome);

        assertThat(pasta.resolve(nome)).doesNotExist();
    }
}
