-- Инициализация БД bank: схемы для каждого микросервиса,
-- реализуя паттерн Database per Service на уровне схем.
-- Скрипт запускается автоматически через docker-entrypoint-initdb.d.

CREATE SCHEMA IF NOT EXISTS accounts AUTHORIZATION bank;
CREATE SCHEMA IF NOT EXISTS cash     AUTHORIZATION bank;
CREATE SCHEMA IF NOT EXISTS transfer AUTHORIZATION bank;

-- Notifications не хранит данные — схема не нужна.
