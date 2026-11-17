package br.org.edu.ifrn.LojaCarro.exception;

// O recurso pedido não existe. Resulta em HTTP 404.
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}
