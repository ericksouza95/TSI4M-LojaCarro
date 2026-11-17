package br.org.edu.ifrn.LojaCarro.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "veiculo")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 80)
    private String modelo;

    @Column(name = "ano_fabricacao", nullable = false)
    private Integer anoFabricacao;

    @Column(name = "ano_modelo", nullable = false)
    private Integer anoModelo;

    @Column(nullable = false, length = 40)
    private String cor;

    @Column(nullable = false)
    private Integer quilometragem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Combustivel combustivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Transmissao transmissao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoVeiculo situacao = SituacaoVeiculo.DISPONIVEL;

    // Nome do arquivo da foto principal, gravado na pasta de imagens (obrigatória)
    @Column(name = "foto_principal", nullable = false)
    private String fotoPrincipal;

    // Usuário (administrador) que cadastrou o veículo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cadastrado_por_id")
    private Usuario cadastradoPor;

    @Column(name = "data_cadastro", nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    void aoCriar() {
        dataCadastro = LocalDateTime.now();
    }

    @PreUpdate
    void aoAtualizar() {
        dataAtualizacao = LocalDateTime.now();
    }

    public boolean isDisponivel() {
        return situacao == SituacaoVeiculo.DISPONIVEL;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAnoFabricacao() {
        return anoFabricacao;
    }

    public void setAnoFabricacao(Integer anoFabricacao) {
        this.anoFabricacao = anoFabricacao;
    }

    public Integer getAnoModelo() {
        return anoModelo;
    }

    public void setAnoModelo(Integer anoModelo) {
        this.anoModelo = anoModelo;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public Integer getQuilometragem() {
        return quilometragem;
    }

    public void setQuilometragem(Integer quilometragem) {
        this.quilometragem = quilometragem;
    }

    public Combustivel getCombustivel() {
        return combustivel;
    }

    public void setCombustivel(Combustivel combustivel) {
        this.combustivel = combustivel;
    }

    public Transmissao getTransmissao() {
        return transmissao;
    }

    public void setTransmissao(Transmissao transmissao) {
        this.transmissao = transmissao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public SituacaoVeiculo getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoVeiculo situacao) {
        this.situacao = situacao;
    }

    public String getFotoPrincipal() {
        return fotoPrincipal;
    }

    public void setFotoPrincipal(String fotoPrincipal) {
        this.fotoPrincipal = fotoPrincipal;
    }

    public Usuario getCadastradoPor() {
        return cadastradoPor;
    }

    public void setCadastradoPor(Usuario cadastradoPor) {
        this.cadastradoPor = cadastradoPor;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
