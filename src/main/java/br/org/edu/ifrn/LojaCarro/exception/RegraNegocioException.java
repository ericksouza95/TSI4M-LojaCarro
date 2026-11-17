package br.org.edu.ifrn.LojaCarro.exception;

// Violação de uma regra de negócio (ex.: e-mail duplicado, ano do modelo inválido). Resulta em HTTP 422.
public class RegraNegocioException extends RuntimeException {
    public RegraNegocioException(String message) {
        super(message);
    }
}
