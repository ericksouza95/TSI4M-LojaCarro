package br.org.edu.ifrn.LojaCarro.exception;

import org.springframework.http.HttpStatus;

// Arquivo enviado recusado: ausente/tipo não permitido (400) ou grande demais (413)
public class ArquivoInvalidoException extends RuntimeException {

    private final HttpStatus status;

    public ArquivoInvalidoException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public ArquivoInvalidoException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
