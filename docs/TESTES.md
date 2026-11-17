# Testes automatizados

| Projeto | Ferramentas | Testes | Cobertura de linhas |
|---|---|---|---|
| API (backend) | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 | **103** (49 unitários + 54 de integração) | **92,4%** (JaCoCo) |
| Microserviço de logs | JUnit 5, Mockito, Spring Boot Test, MockMvc, H2 | **8** (4 unitários + 4 de integração) | **93,5%** (JaCoCo) |
| Frontend (Angular) | Vitest, TestBed, HttpTestingController | **35** (em 11 arquivos) | **50,9%** (v8) |

Números da execução de 17/11/2026, com todos os testes passando.

## Como executar

```bash
# API: testes e relatório de cobertura em target/site/jacoco/index.html
./mvnw test

# Microserviço de logs: relatório em logs-service/target/site/jacoco/index.html
./mvnw -f logs-service/pom.xml test

# Frontend: relatório em frontend/coverage/index.html
cd frontend
npm ci
npm run test:cobertura
```

Os testes do backend **não precisam de MySQL nem de RabbitMQ**:

- O banco é um H2 em memória (modo MySQL), criado pelas **mesmas migrations do Flyway** usadas em produção. O Hibernate valida que as entidades batem com o schema.
- Na API, os eventos de auditoria são capturados em memória (`PublicadorEmMemoria`) em vez de irem para o RabbitMQ.
- Os testes de integração de agendamento usam um relógio fixo (segunda-feira, 04/03/2030, 09:00), para que "data passada", "sábado" e "domingo" não dependam do dia em que o teste roda.

O GitHub Actions ([`.github/workflows/ci.yml`](../.github/workflows/ci.yml)) executa os três projetos a cada push
e publica os relatórios como artefatos.

### Evidências de execução

- **Relatórios por classe de teste:** `target/surefire-reports/` (backend) e a saída do `npm run test:cobertura` (frontend).
- **Cobertura:**
  - `target/site/jacoco/index.html` (API);
  - `logs-service/target/site/jacoco/index.html` (microserviço);
  - `frontend/coverage/index.html` (frontend).

## Principais cenários testados

### Testes unitários (JUnit 5 + Mockito)

| Classe | Cenários |
|---|---|
| `VeiculoServiceTest` (11) | Cadastro vinculado ao admin logado, com foto; ano do modelo anterior ao de fabricação ou 2 anos à frente; ano de fabricação no futuro; atualização; veículo inexistente; exclusão lógica (nunca apaga); troca de foto apaga a antiga; veículo inativo invisível para quem não é admin; faixa de preço invertida. |
| `AgendamentoServiceTest` (8) | Agenda 30 min quando livre, e a verificação de conflito acontece **depois** do bloqueio do veículo; horário ocupado (409); data passada sem consultar o banco; horário que termina após o fechamento; veículo vendido; cancelamento do próprio agendamento; cliente não cancela o de outro; não cancela agendamento que já começou. |
| `ImagemServiceTest` (8) | Aceita JPG/PNG/WebP pelo conteúdo; recusa arquivo com extensão de imagem e conteúdo de outro tipo; recusa extensão não permitida; recusa acima de 5 MB; recusa vazio; nome gerado pelo servidor (ignora `../`); bloqueia leitura fora da pasta. |
| `UsuarioServiceTest` (6) | Cadastro público cria CLIENTE com senha em BCrypt; e-mail duplicado; cliente não acessa dados de outro; cliente acessa os próprios; admin acessa todos; não exclui usuário com agendamentos. |
| `JwtServiceTest` (5) | Token válido devolve o usuário; token de outro servidor (outra chave) é rejeitado; token expirado é rejeitado; texto que não é JWT; segredo curto impede a inicialização. |
| `AuthServiceTest` (3) | Login correto gera token; senha errada não gera token e registra a falha; e-mail inexistente tem a mesma mensagem (não revela quais e-mails existem). |
| `LogServiceTest` (4) | Evento leva usuário, ação e detalhes; autor anônimo; dentro de transação só publica após o commit; transação desfeita não publica. |
| `HorarioFuncionamentoTest` (4) | Grade de 30 em 30 min (semana, sábado, domingo); test drive precisa caber inteiro no expediente; sábado fechado configurável; configuração inválida. |

### Testes de integração (Spring Boot Test + MockMvc + banco H2)

| Classe | Cenários |
|---|---|
| `AgendamentoControllerTest` (19) | **Exemplo do roteiro**: Corolla agendado das 14h às 14h30, outro cliente não agenda às 14h nem às 14h15 e agenda às 15h ou outro veículo às 14h. Horário que começa quando outro termina; cliente com dois test drives ao mesmo tempo; data passada; fora do horário (antes da abertura, depois do fechamento, domingo, sábado à tarde); veículo vendido/inativo; vender cancela os futuros; exige login de cliente; validação; **cancelamento libera o horário**; cliente não cancela o de outro e admin cancela; não cancela agendamento iniciado; cliente vê só os próprios; admin filtra por data e veículo; marcar como realizado; grade de horários livres/ocupados; **8 pedidos simultâneos para o mesmo horário geram exatamente 1 agendamento**. |
| `VeiculoControllerTest` (17) | Cadastro com foto e detalhes públicos; cadastro exige login e perfil admin; cliente não altera nem desativa; validação de campos; ano do modelo; busca por marca/modelo e filtros de preço e ano; exclusão lógica oculta do público; vendido; atualização e 404; auditoria com o autor; cadastro sem foto; PDF disfarçado de `.jpg`; foto acima de 5 MB; foto gravada na pasta e servida pela API; troca de foto apaga a antiga; cliente não troca foto; foto de veículo inativo não é pública. |
| `AuthControllerTest` (8) | Login gera JWT; token dá acesso ao usuário atual; senha errada (401) registrada na auditoria; **endpoint protegido sem token (401)**; token adulterado; token expirado; token de usuário excluído; logout. |
| `UsuarioControllerTest` (8) | CRUD pelo admin; cadastro público não cria admin e guarda a senha em BCrypt; **cliente não acessa dados de outro cliente (403)**; **cliente não acessa endpoints administrativos (403)**; e-mail duplicado; validação; admin não exclui a si mesmo; não exclui usuário com veículos. |
| `LogControllerTest` (2) | Admin consulta os logs do microserviço; microserviço fora do ar (503). |
| `LogsServiceIntegracaoTest` (4, microserviço) | Evento da fila gravado no banco; evento inválido rejeitado para a fila de descarte; consulta exige a chave de acesso; ordenação, busca e limite. |

**Contraprova do teste de concorrência.** O bloqueio pessimista foi removido temporariamente do `AgendamentoService`
e o teste foi executado 3 vezes: nas 3, os 8 pedidos simultâneos foram **todos aceitos** (8 agendamentos no mesmo
horário) e o teste falhou. Com o bloqueio restaurado, o teste passou nas 3 execuções. Isso mostra que o teste
realmente detecta o problema.

### Testes do frontend (Vitest)

| Arquivo | Cenários |
|---|---|
| `veiculo-lista.component.spec.ts` | Renderiza um card por veículo (foto, modelo, preço em R$, link); aplica os filtros da URL; mensagem de lista vazia; aviso quando a API falha. |
| `login.component.spec.ts` | Não envia formulário vazio; valida formato do e-mail; faz login e volta à página pedida; mostra a mensagem da API para senha errada. |
| `veiculo-form.component.spec.ts` | Campos obrigatórios; coerência entre ano de fabricação e do modelo; preço zero e km negativa; não cadastra sem foto; valida tipo e tamanho da foto. |
| `agendar.component.spec.ts` | Mostra a grade e bloqueia horários ocupados; só confirma após escolher horário e envia veículo/data/horário; atualiza a grade quando recebe 409. |
| `veiculo.service.spec.ts` | Envia só os filtros preenchidos; cadastro em multipart com JSON + foto; consulta da grade de horários. |
| `meus-agendamentos.component.spec.ts` | Separa próximos e histórico; cancela após confirmação; não cancela se o cliente desistir. |
| `agendamentos-admin.component.spec.ts` | Mostra cliente, veículo, data, horário e situação; envia filtros de data, veículo e situação; "realizado" só para test drive iniciado. |
| `auth.guards.spec.ts` | Visitante vai ao login guardando a página; cliente não acessa o painel admin; admin e cliente acessam suas áreas. |
| `auth.interceptor.spec.ts` | Envia o token nas chamadas à API; não envia sem login nem para outros domínios. |
| `auth.service.spec.ts` | Guarda token e perfil após o login; descarta sessão com token expirado. |
| `erros.spec.ts` | Usa a mensagem do backend; servidor fora do ar ou erro 500; erros de validação aparecem em cada campo. |

## Sobre a cobertura

- **Backend.** A cobertura é alta porque os testes de integração passam por controllers, services e repositories reais.
  O pacote `mensageria` tem cobertura menor: o envio real ao RabbitMQ e a chamada HTTP ao microserviço não rodam
  nos testes automatizados (seriam necessários os serviços externos). Esse caminho foi verificado executando o
  sistema completo com Docker: 15 cenários de ponta a ponta pela API, com os eventos chegando ao banco do
  microserviço, e uma mensagem inválida indo para a fila de descarte.
- **Frontend.** O percentual considera **todos** os arquivos da aplicação, e não só os carregados pelos testes.
  As regras mais importantes (formulários, agendamento, guards, interceptor, serviços e tratamento de erros) estão
  testadas. As telas que só exibem dados (página inicial, painel, listas de clientes e logs) não têm teste próprio.
