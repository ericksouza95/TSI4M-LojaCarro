package br.org.edu.ifrn.LojaCarro.exception;

// O horário pedido já está ocupado. Resulta em HTTP 409 (Conflict).
public class ConflitoHorarioException extends RuntimeException {
    public ConflitoHorarioException(String message) {
        super(message);
    }
}
