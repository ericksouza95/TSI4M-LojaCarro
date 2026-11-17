package br.org.edu.ifrn.LojaCarro.exception;

// Falha de autenticação (credenciais inválidas ou ausência de login). Resulta em HTTP 401.
public class AutenticacaoException extends RuntimeException {
    public AutenticacaoException(String message) {
        super(message);
    }
}
