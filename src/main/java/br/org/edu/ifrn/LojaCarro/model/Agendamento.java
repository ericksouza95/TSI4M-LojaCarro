package br.org.edu.ifrn.LojaCarro.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Agendamento de test drive (duração fixa de 30 minutos)
@Entity
@Table(name = "agendamento")
public class Agendamento {

    public static final int DURACAO_MINUTOS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAgendamento status = StatusAgendamento.CONFIRMADO;

    // Momento em que o cliente fez o agendamento
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_cancelamento")
    private LocalDateTime dataCancelamento;

    public Agendamento() {
    }

    public Agendamento(Usuario cliente, Veiculo veiculo, LocalDateTime dataHoraInicio) {
        this.cliente = cliente;
        this.veiculo = veiculo;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraInicio.plusMinutes(DURACAO_MINUTOS);
    }

    @PrePersist
    void aoCriar() {
        dataCriacao = LocalDateTime.now();
    }

    public boolean isConfirmado() {
        return status == StatusAgendamento.CONFIRMADO;
    }

    // Só agendamentos confirmados que ainda não começaram podem ser cancelados
    public boolean podeSerCancelado(LocalDateTime agora) {
        return isConfirmado() && dataHoraInicio.isAfter(agora);
    }

    // Cancelar libera o horário: a verificação de conflito só considera agendamentos CONFIRMADOS
    public void cancelar(LocalDateTime agora) {
        status = StatusAgendamento.CANCELADO;
        dataCancelamento = agora;
    }

    public void marcarComoRealizado() {
        status = StatusAgendamento.REALIZADO;
    }

    public Long getId() {
        return id;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public LocalDateTime getDataHoraInicio() {
        return dataHoraInicio;
    }

    public LocalDateTime getDataHoraFim() {
        return dataHoraFim;
    }

    public StatusAgendamento getStatus() {
        return status;
    }

    public void setStatus(StatusAgendamento status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public LocalDateTime getDataCancelamento() {
        return dataCancelamento;
    }

    public void setDataCancelamento(LocalDateTime dataCancelamento) {
        this.dataCancelamento = dataCancelamento;
    }
}
