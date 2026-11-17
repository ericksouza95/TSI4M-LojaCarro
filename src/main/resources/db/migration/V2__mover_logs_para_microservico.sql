-- Os logs de auditoria passaram a ser gravados pelo microservico logs-service (banco lojacarro_logs),
-- que os recebe pelo RabbitMQ. A tabela antiga deixa de existir no banco da loja.
DROP TABLE log_sistema;
