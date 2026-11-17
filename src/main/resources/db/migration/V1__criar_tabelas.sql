-- Criação inicial do banco da Loja de Carros
-- Entidades: usuario, veiculo, agendamento (test drive) e log_sistema (auditoria)

CREATE TABLE usuario (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nome           VARCHAR(120) NOT NULL,
    email          VARCHAR(160) NOT NULL,
    senha          VARCHAR(100) NOT NULL,
    telefone       VARCHAR(20),
    perfil         VARCHAR(20)  NOT NULL,
    data_cadastro  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMIN', 'CLIENTE'))
);

CREATE TABLE veiculo (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    marca              VARCHAR(60)   NOT NULL,
    modelo             VARCHAR(80)   NOT NULL,
    ano_fabricacao     INT           NOT NULL,
    ano_modelo         INT           NOT NULL,
    cor                VARCHAR(40)   NOT NULL,
    quilometragem      INT           NOT NULL,
    combustivel        VARCHAR(20)   NOT NULL,
    transmissao        VARCHAR(20)   NOT NULL,
    preco              DECIMAL(12,2) NOT NULL,
    descricao          VARCHAR(2000),
    situacao           VARCHAR(20)   NOT NULL,
    foto_principal     VARCHAR(255)  NOT NULL,
    cadastrado_por_id  BIGINT,
    data_cadastro      DATETIME(6)   NOT NULL,
    data_atualizacao   DATETIME(6),
    CONSTRAINT pk_veiculo PRIMARY KEY (id),
    CONSTRAINT fk_veiculo_cadastrado_por FOREIGN KEY (cadastrado_por_id) REFERENCES usuario (id),
    CONSTRAINT ck_veiculo_preco CHECK (preco > 0),
    CONSTRAINT ck_veiculo_quilometragem CHECK (quilometragem >= 0),
    CONSTRAINT ck_veiculo_anos CHECK (ano_modelo >= ano_fabricacao),
    CONSTRAINT ck_veiculo_combustivel CHECK (combustivel IN ('GASOLINA', 'ETANOL', 'FLEX', 'DIESEL', 'ELETRICO', 'HIBRIDO')),
    CONSTRAINT ck_veiculo_transmissao CHECK (transmissao IN ('MANUAL', 'AUTOMATICA', 'AUTOMATIZADA', 'CVT')),
    CONSTRAINT ck_veiculo_situacao CHECK (situacao IN ('DISPONIVEL', 'VENDIDO', 'INATIVO'))
);

CREATE INDEX idx_veiculo_marca_modelo ON veiculo (marca, modelo);
CREATE INDEX idx_veiculo_situacao ON veiculo (situacao);

CREATE TABLE agendamento (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    cliente_id         BIGINT      NOT NULL,
    veiculo_id         BIGINT      NOT NULL,
    data_hora_inicio   DATETIME(6) NOT NULL,
    data_hora_fim      DATETIME(6) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    data_criacao       DATETIME(6) NOT NULL,
    data_cancelamento  DATETIME(6),
    CONSTRAINT pk_agendamento PRIMARY KEY (id),
    CONSTRAINT fk_agendamento_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id),
    CONSTRAINT fk_agendamento_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculo (id),
    CONSTRAINT ck_agendamento_periodo CHECK (data_hora_fim > data_hora_inicio),
    CONSTRAINT ck_agendamento_status CHECK (status IN ('CONFIRMADO', 'CANCELADO', 'REALIZADO'))
);

CREATE INDEX idx_agendamento_veiculo_inicio ON agendamento (veiculo_id, data_hora_inicio);
CREATE INDEX idx_agendamento_cliente ON agendamento (cliente_id);

CREATE TABLE log_sistema (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    data_hora  DATETIME(6)   NOT NULL,
    acao       VARCHAR(40)   NOT NULL,
    entidade   VARCHAR(40)   NOT NULL,
    descricao  VARCHAR(1000),
    usuario    VARCHAR(200),
    CONSTRAINT pk_log_sistema PRIMARY KEY (id)
);
