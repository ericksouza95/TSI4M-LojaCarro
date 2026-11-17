package br.org.edu.ifrn.LojaCarro.exception;

// O usuário está autenticado, mas não pode acessar o recurso (ex.: dados de outro cliente). Resulta em HTTP 403.
public class AcessoNegadoException extends RuntimeException {
    public AcessoNegadoException(String message) {
        super(message);
    }
}
