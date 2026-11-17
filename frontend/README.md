# LojaCarro — Frontend (Angular)

Interface web da loja: catálogo público, cadastro/login, agendamento de test drive e área administrativa.

## Requisitos

- Node.js 22.12+ ou 24+
- Backend rodando em `http://localhost:8080` (ver README da raiz)

## Executar em desenvolvimento

```bash
npm install
npm start
```

Acesse `http://localhost:4200`. As chamadas para `/api` são encaminhadas ao backend pelo proxy (`proxy.conf.json`), então não é preciso configurar CORS em desenvolvimento.

## Testes

```bash
npm test -- --watch=false
```

## Build de produção

```bash
npm run build
```

Os arquivos ficam em `dist/loja-carro-web/browser`.

## Organização

```
src/app/
├── core/
│   ├── models/        tipos que espelham os DTOs da API
│   ├── services/      comunicação com a API e estado de autenticação
│   ├── interceptors/  token JWT, sessão expirada e indicador de carregamento
│   ├── guards/        proteção das rotas por perfil (visitante, cliente, administrador)
│   └── utils/         tratamento de erros da API e datas
├── shared/            componentes e pipes reutilizáveis
├── pages/             telas (públicas, cliente e administrador)
└── app.routes.ts      rotas com carregamento sob demanda
```
