# Diagrama entidade-relacionamento (DER)

O sistema usa **dois bancos** no mesmo servidor MySQL 8.4:

- `lojacarro`: dados da loja (usuários, veículos e agendamentos), usado pela API.
- `lojacarro_logs`: logs de auditoria, usado somente pelo microserviço de logs.

Os dois são criados e versionados pelo **Flyway**: os scripts ficam em
[`src/main/resources/db/migration`](../src/main/resources/db/migration) e
[`logs-service/src/main/resources/db/migration`](../logs-service/src/main/resources/db/migration).

## Banco `lojacarro`

```mermaid
erDiagram
    USUARIO ||--o{ AGENDAMENTO : "agenda (cliente)"
    VEICULO ||--o{ AGENDAMENTO : "recebe test drive"
    USUARIO |o--o{ VEICULO : "cadastra (administrador)"

    USUARIO {
        bigint id PK
        varchar(120) nome "NOT NULL"
        varchar(160) email UK "NOT NULL"
        varchar(100) senha "hash BCrypt"
        varchar(20) telefone "opcional"
        varchar(20) perfil "ADMIN ou CLIENTE"
        datetime data_cadastro "NOT NULL"
    }

    VEICULO {
        bigint id PK
        varchar(60) marca "NOT NULL"
        varchar(80) modelo "NOT NULL"
        int ano_fabricacao "NOT NULL"
        int ano_modelo "maior ou igual ao de fabricacao"
        varchar(40) cor "NOT NULL"
        int quilometragem "maior ou igual a 0"
        varchar(20) combustivel "GASOLINA ETANOL FLEX DIESEL ELETRICO HIBRIDO"
        varchar(20) transmissao "MANUAL AUTOMATICA AUTOMATIZADA CVT"
        decimal preco "maior que 0"
        varchar(2000) descricao "opcional"
        varchar(20) situacao "DISPONIVEL VENDIDO INATIVO"
        varchar(255) foto_principal "arquivo na pasta de imagens, NOT NULL"
        bigint cadastrado_por_id FK
        datetime data_cadastro "NOT NULL"
        datetime data_atualizacao
    }

    AGENDAMENTO {
        bigint id PK
        bigint cliente_id FK "NOT NULL"
        bigint veiculo_id FK "NOT NULL"
        datetime data_hora_inicio "NOT NULL"
        datetime data_hora_fim "inicio + 30 min"
        varchar(20) status "CONFIRMADO CANCELADO REALIZADO"
        datetime data_criacao "momento do agendamento"
        datetime data_cancelamento
    }
```

### Relacionamentos

| Relação | Cardinalidade | Significado |
|---|---|---|
| Usuário → Agendamento | 1 : N | Um cliente pode ter vários test drives; cada agendamento pertence a um cliente. |
| Veículo → Agendamento | 1 : N | Um veículo recebe vários test drives ao longo do tempo. |
| Usuário → Veículo | 0..1 : N | O administrador que cadastrou o veículo. |

### Restrições de integridade

| Restrição | Tabela | Regra |
|---|---|---|
| `uk_usuario_email` | usuario | E-mail único. |
| `ck_usuario_perfil` | usuario | Perfil é `ADMIN` ou `CLIENTE`. |
| `fk_veiculo_cadastrado_por` | veiculo | Referencia `usuario(id)`. |
| `ck_veiculo_preco` | veiculo | `preco > 0`. |
| `ck_veiculo_quilometragem` | veiculo | `quilometragem >= 0`. |
| `ck_veiculo_anos` | veiculo | `ano_modelo >= ano_fabricacao`. |
| `ck_veiculo_combustivel`, `ck_veiculo_transmissao`, `ck_veiculo_situacao` | veiculo | Apenas os valores válidos de cada enum. |
| `fk_agendamento_cliente`, `fk_agendamento_veiculo` | agendamento | Referenciam `usuario(id)` e `veiculo(id)`. |
| `ck_agendamento_periodo` | agendamento | `data_hora_fim > data_hora_inicio`. |
| `ck_agendamento_status` | agendamento | `CONFIRMADO`, `CANCELADO` ou `REALIZADO`. |

Índices: `veiculo (marca, modelo)`, `veiculo (situacao)`, `agendamento (veiculo_id, data_hora_inicio)` e
`agendamento (cliente_id)`, usados pelas buscas do catálogo e pela verificação de conflito de horário.

**Mecanismos contra inconsistências** além das restrições:

- **Exclusão lógica de veículos.** Desativar um veículo muda a situação para `INATIVO`, então o histórico de agendamentos é preservado.
- **Usuários com vínculos.** Usuários que cadastraram veículos ou têm agendamentos não podem ser excluídos.
- **Conflito de horário.** É verificado com bloqueio pessimista da linha do veículo (`SELECT ... FOR UPDATE`), o que impede dois agendamentos sobrepostos mesmo com pedidos simultâneos.

## Banco `lojacarro_logs` (microserviço de logs)

```mermaid
erDiagram
    LOG_AUDITORIA {
        bigint id PK
        datetime data_hora "quando a operacao aconteceu"
        varchar(40) acao "CRIAR ATUALIZAR CANCELAR LOGIN ..."
        varchar(40) entidade "Veiculo Agendamento Usuario ..."
        varchar(1000) descricao "detalhes da operacao"
        varchar(200) usuario "nome e e-mail ou anonimo"
        varchar(60) origem "servico que gerou o evento"
        datetime recebido_em "quando o microservico recebeu"
    }
```

A tabela não tem chave estrangeira para `usuario`: o autor é guardado como texto, para o histórico continuar legível
mesmo que o usuário seja excluído, e porque os bancos dos dois serviços são independentes.
