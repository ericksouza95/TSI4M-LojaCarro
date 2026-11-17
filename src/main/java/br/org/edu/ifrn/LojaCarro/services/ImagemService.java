package br.org.edu.ifrn.LojaCarro.services;

import br.org.edu.ifrn.LojaCarro.exception.ArquivoInvalidoException;
import br.org.edu.ifrn.LojaCarro.exception.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// Validação, gravação e leitura das fotografias dos veículos na pasta de imagens
@Service
public class ImagemService {

    private static final Logger logger = LoggerFactory.getLogger(ImagemService.class);

    public static final long TAMANHO_MAXIMO_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Set<String> EXTENSOES_PERMITIDAS = Set.of("jpg", "jpeg", "png", "webp");

    private final Path diretorio;

    public ImagemService(@Value("${loja.imagens.diretorio}") String diretorio) {
        this.diretorio = Paths.get(diretorio).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.diretorio);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível criar a pasta de imagens " + this.diretorio, e);
        }
    }

    // Formatos aceitos, identificados pela "assinatura" (primeiros bytes) do arquivo
    enum TipoImagem {
        JPEG("jpg", MediaType.IMAGE_JPEG),
        PNG("png", MediaType.IMAGE_PNG),
        WEBP("webp", MediaType.parseMediaType("image/webp"));

        final String extensao;
        final MediaType mediaType;

        TipoImagem(String extensao, MediaType mediaType) {
            this.extensao = extensao;
            this.mediaType = mediaType;
        }

        static TipoImagem detectar(byte[] b) {
            if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
                return JPEG;
            }
            byte[] assinaturaPng = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
            if (b.length >= 8 && Arrays.equals(Arrays.copyOf(b, 8), assinaturaPng)) {
                return PNG;
            }
            if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
                return WEBP;
            }
            return null;
        }

        static TipoImagem porExtensao(String nomeArquivo) {
            String extensao = extensaoDe(nomeArquivo);
            for (TipoImagem tipo : values()) {
                if (tipo.extensao.equals(extensao)) {
                    return tipo;
                }
            }
            return null;
        }
    }

    // Valida e grava a imagem com um nome gerado pelo servidor. Retorna o nome do arquivo gravado.
    public String salvar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("A fotografia do veículo é obrigatória.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoInvalidoException("A imagem deve ter no máximo 5 MB.", HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String extensaoOriginal = extensaoDe(arquivo.getOriginalFilename());
        if (!EXTENSOES_PERMITIDAS.contains(extensaoOriginal)) {
            throw new ArquivoInvalidoException("Formato não permitido. Envie uma imagem JPG, JPEG, PNG ou WebP.");
        }

        TipoImagem tipo = detectarTipo(arquivo);
        if (tipo == null) {
            throw new ArquivoInvalidoException("O conteúdo do arquivo não é uma imagem JPG, PNG ou WebP válida.");
        }

        String nome = UUID.randomUUID() + "." + tipo.extensao;
        try (InputStream entrada = arquivo.getInputStream()) {
            Files.copy(entrada, resolver(nome));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar a imagem.", e);
        }
        return nome;
    }

    public Resource carregar(String nome) {
        Path caminho = resolver(nome);
        if (!Files.isRegularFile(caminho)) {
            throw new RecursoNaoEncontradoException("Imagem não encontrada.");
        }
        return new PathResource(caminho);
    }

    public MediaType tipoDe(String nome) {
        TipoImagem tipo = TipoImagem.porExtensao(nome);
        return tipo != null ? tipo.mediaType : MediaType.APPLICATION_OCTET_STREAM;
    }

    public void excluir(String nome) {
        if (nome == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(nome));
        } catch (IOException | ArquivoInvalidoException e) {
            logger.warn("Não foi possível excluir a imagem {}", nome, e);
        }
    }

    // Garante que o caminho final fica dentro da pasta de imagens (bloqueia "../")
    private Path resolver(String nome) {
        Path caminho = diretorio.resolve(nome).normalize();
        if (!caminho.getParent().equals(diretorio)) {
            throw new ArquivoInvalidoException("Nome de arquivo inválido.");
        }
        return caminho;
    }

    private TipoImagem detectarTipo(MultipartFile arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            return TipoImagem.detectar(entrada.readNBytes(12));
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler o arquivo enviado.");
        }
    }

    private static String extensaoDe(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            return "";
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
