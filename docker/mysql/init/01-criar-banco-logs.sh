#!/bin/bash
# Executado pelo container do MySQL apenas na primeira inicializacao (volume vazio).
# Cria o banco do microservico de logs e da acesso a ele ao mesmo usuario da aplicacao.
set -e
mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS lojacarro_logs CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON lojacarro_logs.* TO '$MYSQL_USER'@'%';
FLUSH PRIVILEGES;
SQL
