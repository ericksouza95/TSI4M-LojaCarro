package br.org.edu.ifrn.LojaCarro.exception;

import br.org.edu.ifrn.LojaCarro.dto.ErroResponse;
import br.org.edu.ifrn.LojaCarro.services.LogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// Converte as exceções em respostas HTTP com o mesmo formato (ErroResponse)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final LogService logService;

    public GlobalExceptionHandler(LogService logService) {
        this.logService = logService;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        logService.registrar("ERRO", "Validacao", req.getMethod() + " " + req.getRequestURI() + ": " + campos);
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos.", campos, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado.", null, req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + ex.getName() + "'.", null, req);
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse> arquivoInvalido(ArquivoInvalidoException ex, HttpServletRequest req) {
        logService.registrar("ERRO", "Upload", req.getMethod() + " " + req.getRequestURI() + ": " + ex.getMessage());
        return resposta(ex.getStatus(), ex.getMessage(), null, req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> arquivoGrandeDemais(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, "A imagem deve ter no máximo 5 MB.", null, req);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> parteAusente(MissingServletRequestPartException ex, HttpServletRequest req) {
        String mensagem = "foto".equals(ex.getRequestPartName())
                ? "A fotografia do veículo é obrigatória."
                : "A parte '" + ex.getRequestPartName() + "' da requisição é obrigatória.";
        return resposta(HttpStatus.BAD_REQUEST, mensagem, null, req);
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErroResponse> multipartInvalido(MultipartException ex, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Envio de arquivo inválido. Use multipart/form-data.", null, req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tipoNaoSuportado(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado: " + ex.getContentType(), null, req);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException ex, HttpServletRequest req) {
        logService.registrar("ERRO", "RegraNegocio", ex.getMessage());
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null, req);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), null, req);
    }

    @ExceptionHandler(AutenticacaoException.class)
    public ResponseEntity<ErroResponse> autenticacao(AutenticacaoException ex, HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, ex.getMessage(), null, req);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException ex, HttpServletRequest req) {
        logService.registrar("ACESSO_PROIBIDO", "Sistema", req.getMethod() + " " + req.getRequestURI() + ": " + ex.getMessage());
        return resposta(HttpStatus.FORBIDDEN, ex.getMessage(), null, req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegadoSpring(AccessDeniedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, "Você não tem permissão para realizar esta operação.", null, req);
    }

    // Última barreira: uma restrição do banco (unique, FK, check) foi violada
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> integridade(DataIntegrityViolationException ex, HttpServletRequest req) {
        logger.warn("Violação de integridade em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return resposta(HttpStatus.CONFLICT, "A operação viola uma restrição de integridade dos dados.", null, req);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> metodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método " + ex.getMethod() + " não suportado neste endereço.", null, req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, "Endereço não encontrado.", null, req);
    }

    // Erros inesperados: detalhes ficam só no log do servidor, nunca na resposta
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> erroInesperado(Exception ex, HttpServletRequest req) {
        logger.error("Erro inesperado em {} {}", req.getMethod(), req.getRequestURI(), ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor.", null, req);
    }

    private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem, Map<String, String> campos,
                                                  HttpServletRequest req) {
        ErroResponse corpo = new ErroResponse(status.value(), mensagem, campos, req.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(status).body(corpo);
    }
}
