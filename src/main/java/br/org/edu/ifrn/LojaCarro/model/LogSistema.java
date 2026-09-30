package br.org.edu.ifrn.LojaCarro.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "log_sistema")
public class LogSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime dataHora;
    private String acao;
    private String entidade;
    @Column(length = 1000)
    private String descricao;

    public LogSistema() {
    }

    public LogSistema(String acao, String entidade, String descricao) {
        this.dataHora = LocalDateTime.now();
        this.acao = acao;
        this.entidade = entidade;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getAcao() {
        return acao;
    }

    public String getEntidade() {
        return entidade;
    }

    public String getDescricao() {
        return descricao;
    }
}
