-- Banco do microserviço de logs de auditoria
CREATE TABLE log_auditoria (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    data_hora    DATETIME(6)   NOT NULL,
    acao         VARCHAR(40)   NOT NULL,
    entidade     VARCHAR(40)   NOT NULL,
    descricao    VARCHAR(1000),
    usuario      VARCHAR(200)  NOT NULL,
    origem       VARCHAR(60)   NOT NULL,
    recebido_em  DATETIME(6)   NOT NULL,
    CONSTRAINT pk_log_auditoria PRIMARY KEY (id)
);

CREATE INDEX idx_log_auditoria_data_hora ON log_auditoria (data_hora);
CREATE INDEX idx_log_auditoria_acao ON log_auditoria (acao);
