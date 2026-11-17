# Publicação na AWS EC2

Todo o sistema sobe com um único comando, usando o `docker-compose.prod.yml`:

- banco MySQL;
- RabbitMQ;
- API;
- microserviço de logs;
- frontend com Nginx.

Só a porta **80** fica aberta para a internet.

## Requisitos da instância

- **Sistema:** Ubuntu 22.04/24.04 ou Amazon Linux 2023.
- **Memória:** os cinco containers usam cerca de **1,1 GB** de RAM (medido com os ajustes de memória do compose).
  Use pelo menos **2 GB** (ex.: `t3.small`). Numa `t2.micro`/`t3.micro` (1 GB), crie um arquivo de swap de 2 GB
  (comandos abaixo).
- **Disco:** cerca de 4 GB livres para as imagens Docker.
- **Security Group:** liberar a entrada **TCP 80** (HTTP) e **TCP 22** (SSH, de preferência só para o seu IP).

## Passo a passo

```bash
# 1. Acessar a instância
ssh -i minha-chave.pem ubuntu@<IP-PUBLICO>

# 2. Instalar Docker e o plugin compose (Ubuntu)
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER && newgrp docker

# (Opcional, instâncias com 1 GB) criar swap de 2 GB
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 3. Baixar o projeto
git clone https://github.com/ericksouza95/TSI4M-LojaCarro.git
cd TSI4M-LojaCarro

# 4. Configurar as variáveis (senhas fortes e aleatórias)
cp .env.prod.example .env
nano .env        # preencha todas as senhas; em CORS_ORIGINS use http://<IP-PUBLICO>
# Dica para gerar valores:  openssl rand -base64 32

# 5. Construir e subir tudo (a primeira vez leva alguns minutos)
docker compose -f docker-compose.prod.yml up -d --build

# 6. Acompanhar a inicialização
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f api
```

Depois que a API registrar `Started LojaCarroApplication`, acesse:

- **Sistema:** `http://<IP-PUBLICO>`
- **Documentação da API:** `http://<IP-PUBLICO>/swagger-ui.html`

O administrador é criado na primeira inicialização com `ADMIN_EMAIL`/`ADMIN_SENHA` do `.env`. Se você preencher
`DEMO_CLIENTE_EMAIL`/`DEMO_CLIENTE_SENHA`, um cliente de demonstração também é criado.

## Operação

```bash
# Atualizar para a versão mais nova do repositório
git pull
docker compose -f docker-compose.prod.yml up -d --build

# Parar (os dados continuam nos volumes)
docker compose -f docker-compose.prod.yml down

# Ver logs de um serviço
docker compose -f docker-compose.prod.yml logs --tail 100 logs-service

# Backup do banco
docker compose -f docker-compose.prod.yml exec mysql \
  sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --databases lojacarro lojacarro_logs' > backup.sql
```

## Onde ficam os dados

| Dado | Onde |
|---|---|
| Bancos `lojacarro` e `lojacarro_logs` | Volume Docker `mysql-dados` |
| Fotografias dos veículos (pasta IMG) | Volume Docker `imagens` (montado em `/app/img` na API) |
| Mensagens pendentes de auditoria | Filas duráveis do RabbitMQ |

O comando `docker compose down -v` **apaga os volumes**, e com eles todos os dados. Use só se quiser recomeçar do zero.

## Checklist da demonstração

- [ ] `http://<IP-PUBLICO>` abre a página inicial
- [ ] Login como administrador e cadastro de um veículo com foto
- [ ] Criação de conta de cliente, agendamento de test drive e tentativa de horário conflitante (deve ser recusada)
- [ ] Cancelamento libera o horário
- [ ] Painel e tela de agendamentos do administrador (filtros por data e veículo)
- [ ] Tela de logs mostrando as operações (vindas do microserviço via RabbitMQ)
- [ ] Cliente tentando acessar `/admin` é bloqueado
