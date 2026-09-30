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
    // Guardado como texto para o histórico continuar legível mesmo se o usuário for excluído
    private String usuario;

    public LogSistema() {
    }

    public LogSistema(String acao, String entidade, String descricao, String usuario) {
        this.dataHora = LocalDateTime.now();
        this.acao = acao;
        this.entidade = entidade;
        this.descricao = descricao;
        this.usuario = usuario;
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

    public String getUsuario() {
        return usuario;
    }
}
