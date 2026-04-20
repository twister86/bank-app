-- Инициализация БД bank: схемы для каждого микросервиса,
-- Скрипт запускается автоматически через docker-entrypoint-initdb.d.

CREATE SCHEMA IF NOT EXISTS accounts AUTHORIZATION bank;
CREATE SCHEMA IF NOT EXISTS cash     AUTHORIZATION bank;
CREATE SCHEMA IF NOT EXISTS transfer AUTHORIZATION bank;

