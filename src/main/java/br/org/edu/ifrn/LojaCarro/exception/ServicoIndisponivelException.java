package br.org.edu.ifrn.LojaCarro.exception;

// Um serviço externo (ex.: microserviço de logs) não respondeu. Resulta em HTTP 503.
public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String message) {
        super(message);
    }
}
