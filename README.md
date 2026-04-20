# My Bank — микросервисное приложение 

## Состав

| Модуль                   | Порт  | Назначение                            |
|--------------------------|-------|---------------------------------------|
| `eureka-server`          | 8761  | Service Discovery (Netflix Eureka)    |
| `config-server`          | 8888  | Externalized Config (native profile)  |
| `gateway`                | 8080  | Spring Cloud Gateway MVC + TokenRelay |
| `accounts-service`       | 8081  | Аккаунты, балансы (PostgreSQL)        |
| `cash-service`           | 8082  | Пополнение/снятие                     |
| `transfer-service`       | 8083  | Переводы между счетами                |
| `notifications-service`  | 8084  | Логирование операций                  |
| `front`                  | 8085  | Front UI (Thymeleaf + OAuth2 Login)   |
| `postgres`               | 5432  | БД с тремя схемами                    |
| `keycloak`               | 8180  | OAuth 2.0 Authorization Server        |

## Архитектура

```
Browser ──► Front UI ──► Gateway ──► accounts / cash / transfer
                                      │          │         │
                                      └──► notifications ◄─┘

Пользовательские запросы: Authorization Code Flow через Keycloak.
Фронт получает JWT и проксирует через Gateway (TokenRelay пробрасывает
заголовок Authorization в downstream).

Межсервисные вызовы: Client Credentials Flow. Каждый сервис имеет свой
OAuth2 клиент в Keycloak со своим набором scope'ов.
```

### OAuth клиенты

| Клиент            | Flow                  | Scopes                                                          |
|-------------------|-----------------------|-----------------------------------------------------------------|
| `front-client`    | Authorization Code    | `accounts.read/write`, `cash.write`, `transfer.write`           |
| `gateway-client`  | Authorization Code    | те же                                                           |
| `accounts-client` | Client Credentials    | `notifications.write`                                           |
| `cash-client`     | Client Credentials    | `accounts.write`, `notifications.write`                         |
| `transfer-client` | Client Credentials    | `accounts.write`, `notifications.write`                         |

### Реализованные микросервисные паттерны

- **Service Discovery** — Eureka
- **Externalized Config** — Spring Cloud Config Server (native profile)
- **API Gateway** — Spring Cloud Gateway MVC
- **Access Token** — JWT от Keycloak
- **Token Relay** — Gateway пробрасывает user JWT в downstream
- **UI Composition** — Front агрегирует данные из Accounts
- **Database per Service** — одна БД, схема на сервис
- **Contract Testing** — Spring Cloud Contract между accounts (producer) и cash (consumer)
- **RPI** — REST-вызовы между сервисами
- **Compensating Transaction** — в TransferService откат при ошибке deposit

### Схема БД

Одна база `bank`, три схемы:
- `accounts.accounts` — id, login (unique), name, birthdate, balance, version
- `cash`, `transfer` — зарезервированы, сервисы stateless

## Запуск

### Через Docker Compose

```bash
docker compose up -d --build
```

### Проверка готовности

| URL                                                                            | Что это                          |
|--------------------------------------------------------------------------------|----------------------------------|
| http://localhost:8761                                                          | Eureka Dashboard                 |
| http://localhost:8888/accounts-service/default                                 | Конфиг Accounts от Config Server |
| http://localhost:8180 (admin/admin)                                            | Keycloak Admin Console           |
| http://localhost:8180/realms/my-bank/.well-known/openid-configuration          | Realm metadata                   |
| **http://localhost:8085**                                                      | **Front UI — стартовая точка**   |

### Тестовые пользователи (пароль `password` у всех)
- `ivan` — Иванов Иван, баланс 1000 ₽
- `petr` — Петров Пётр, баланс 500 ₽
- `sidor` — Сидоров Сидор, баланс 2000 ₽

### Ручная проверка через curl

```bash
# Получить access token (direct-grant)
TOKEN=$(curl -s -X POST \
  "http://localhost:8180/realms/my-bank/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=front-client" \
  -d "client_secret=front-secret" \
  -d "username=ivan" -d "password=password" \
  -d "scope=accounts.read accounts.write cash.write transfer.write" \
  | jq -r .access_token)

# Через Gateway:
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/accounts/me
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"amount":500,"action":"PUT"}' http://localhost:8080/api/cash
curl -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"toLogin":"petr","amount":100}' http://localhost:8080/api/transfer
```

> Direct-grant (`password`) для `front-client` по умолчанию выключен. Для curl-проверок
> временно включите в Admin Console: Clients → front-client → "Direct access grants enabled: On".

## Локальный запуск (без Docker)

```bash
docker compose up -d postgres keycloak
# В отдельных терминалах:
mvn -pl eureka-server spring-boot:run
mvn -pl config-server spring-boot:run
mvn -pl accounts-service spring-boot:run
mvn -pl notifications-service spring-boot:run
mvn -pl cash-service spring-boot:run
mvn -pl transfer-service spring-boot:run
mvn -pl gateway spring-boot:run
mvn -pl front spring-boot:run
```

## Тестирование

```bash
mvn clean verify                                   # все тесты
mvn -pl accounts-service test                      # unit + IT одного модуля
mvn -pl accounts-service clean install             # producer-side contract tests
mvn -pl cash-service test                          # consumer-side contract tests
```

### Тестовая пирамида

| Уровень      | Модули                                  | Инструменты                               |
|--------------|-----------------------------------------|-------------------------------------------|
| Unit         | accounts, cash, transfer                | Mockito                                   |
| Integration  | accounts, notifications, gateway, front | @SpringBootTest, Testcontainers, mock JWT |
| Contract     | accounts (producer) ↔ cash (consumer)   | Spring Cloud Contract (2 груви-контракта) |

## Структура проекта

```
my-bank-app/
├── pom.xml                        # Parent, Spring Cloud 2025.0.0 BOM
├── docker-compose.yml
├── README.md
├── docker/init-db.sql
├── keycloak/my-bank-realm.json
├── eureka-server/
├── config-server/
│   └── src/main/resources/configs/     # централизованные yaml
├── gateway/
├── accounts-service/
│   ├── src/main/resources/db/changelog/
│   └── src/test/resources/contracts/   # SCC producer contracts
├── cash-service/
├── transfer-service/
├── notifications-service/
└── front/
```
