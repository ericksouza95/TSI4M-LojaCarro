# Endpoints REST

A documentação interativa (Swagger/OpenAPI) fica em **`/swagger-ui.html`**: http://localhost:8080/swagger-ui.html em
desenvolvimento, ou `http://<endereço>/swagger-ui.html` na publicação. Para testar rotas protegidas, faça login,
copie o `token` e use o botão **Authorize**.

Legenda de acesso:
- **Público**: não precisa de login.
- **Logado**: qualquer usuário com token válido.
- **Cliente**: perfil `CLIENTE`.
- **Admin**: perfil `ADMIN`.

## Autenticação — `/api/auth`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | Público | Login com e-mail e senha. Devolve `{ token, tipo, expiraEm, usuario }`. |
| POST | `/api/auth/logout` | Logado | Registra a saída na auditoria (o frontend descarta o token). |
| GET | `/api/auth/me` | Logado | Dados do usuário dono do token. |

## Usuários — `/api/usuarios`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/usuarios` | Público | Cria conta. Sempre com perfil **CLIENTE**. |
| GET | `/api/usuarios` | Admin | Lista os usuários (informações dos clientes). |
| GET | `/api/usuarios/{id}` | Logado | O cliente só vê os próprios dados (403 para os de outro); o admin vê todos. |
| PUT | `/api/usuarios/{id}` | Logado | Atualiza nome, e-mail, telefone e senha (senha vazia mantém a atual). Mesma regra de dono. |
| DELETE | `/api/usuarios/{id}` | Admin | Exclui usuário sem veículos nem agendamentos. |

## Veículos — `/api/veiculos`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/veiculos?busca=&precoMin=&precoMax=&anoMin=&anoMax=&situacao=` | Público | Catálogo com filtros. Visitantes e clientes veem só `DISPONIVEL`; o filtro `situacao` vale apenas para o admin. |
| GET | `/api/veiculos/{id}` | Público | Detalhes. Veículo inativo responde 404 para quem não é admin. |
| GET | `/api/veiculos/{id}/foto` | Público | Fotografia principal (`image/jpeg`, `image/png` ou `image/webp`). |
| GET | `/api/veiculos/{id}/horarios?data=AAAA-MM-DD` | Público | Grade de horários de test drive do dia (30 em 30 min), com `disponivel: true/false`. |
| POST | `/api/veiculos` | Admin | Cadastro em `multipart/form-data`: parte `dados` (JSON) e parte `foto` (obrigatória, até 5 MB). |
| PUT | `/api/veiculos/{id}` | Admin | Atualiza os dados (JSON). |
| PUT | `/api/veiculos/{id}/foto` | Admin | Troca a foto (`multipart/form-data`, parte `foto`). |
| PATCH | `/api/veiculos/{id}/situacao` | Admin | `{ "situacao": "DISPONIVEL" \| "VENDIDO" \| "INATIVO" }`. Vendido/inativo cancela os test drives futuros. |
| DELETE | `/api/veiculos/{id}` | Admin | Exclusão **lógica** (situação `INATIVO`). |

Exemplo de cadastro com `curl`:

```bash
curl -X POST http://localhost:8080/api/veiculos \
  -H "Authorization: Bearer $TOKEN" \
  -F 'dados={"marca":"Toyota","modelo":"Corolla","anoFabricacao":2022,"anoModelo":2023,"cor":"Prata","quilometragem":28000,"combustivel":"FLEX","transmissao":"CVT","preco":129900,"descricao":"Revisado"};type=application/json' \
  -F "foto=@corolla.jpg;type=image/jpeg"
```

## Agendamentos — `/api/agendamentos`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/agendamentos` | Cliente | `{ "veiculoId": 1, "data": "2030-03-05", "horario": "14:00" }`. Duração de 30 min. **409** se o horário estiver ocupado. |
| GET | `/api/agendamentos/meus` | Logado | Agendamentos do usuário logado. |
| GET | `/api/agendamentos?data=&veiculoId=&status=` | Admin | Todos os agendamentos, com filtros por data, veículo e situação. |
| GET | `/api/agendamentos/{id}` | Logado | Detalhes (dono ou admin). |
| PATCH | `/api/agendamentos/{id}/cancelar` | Logado | Cancela agendamento futuro (o cliente cancela os próprios; o admin, qualquer um). Libera o horário. |
| PATCH | `/api/agendamentos/{id}/realizado` | Admin | Marca como realizado um test drive que já começou. |

## Logs — `/api/logs`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/api/logs?busca=` | Admin | Logs de auditoria, buscados no microserviço de logs. **503** se ele estiver fora do ar. |

Microserviço de logs (interno, porta 8081, não exposto na publicação):

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/logs?busca=&limite=` | Cabeçalho `X-Api-Key` | Consulta usada pela API. |

## Respostas de erro

Todas as respostas de erro usam o mesmo formato:

```json
{
  "status": 400,
  "erro": "Dados inválidos.",
  "campos": { "preco": "O preço deve ser maior que zero." },
  "caminho": "/api/veiculos",
  "dataHora": "2030-03-05T10:00:00"
}
```

| Código | Quando |
|---|---|
| 400 | Dados inválidos (com `campos`), JSON mal formatado, foto ausente ou de tipo inválido. |
| 401 | Sem login, token inválido/expirado, ou e-mail/senha errados. |
| 403 | Perfil sem permissão ou acesso a dados de outro usuário. |
| 404 | Recurso inexistente. |
| 409 | Horário de test drive já ocupado, ou violação de integridade no banco. |
| 413 | Imagem maior que 5 MB. |
| 415 | Tipo de conteúdo não suportado (ex.: JSON onde se espera multipart). |
| 422 | Regra de negócio (ex.: ano do modelo inválido, data passada, fora do horário da loja, veículo vendido). |
| 503 | Microserviço de logs indisponível. |
