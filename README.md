# My Bank — микросервисное приложение (Sprint 9)

Учебный проект по курсу Яндекс.Практикум: микросервисный «Банк» с
аутентификацией пользователя через OAuth 2.0 Authorization Code Flow
и межсервисной авторизацией через Client Credentials Flow.

## Статус проекта

| Модуль                   | Порт  | Назначение                                      |
|--------------------------|-------|-------------------------------------------------|
| `eureka-server`          | 8761  | Service Discovery (Netflix Eureka)              |
| `config-server`          | 8888  | Externalized Config (native profile)            |
| `gateway`                | 8080  | Spring Cloud Gateway MVC + TokenRelay           |
| `accounts-service`       | 8081  | Аккаунты, балансы                               |
| `cash-service`           | 8082  | Пополнение/снятие                               |
| `transfer-service`       | 8083  | Переводы между счетами                          |
| `notifications-service`  | 8084  | Логирование операций                            |
| `front`                  | 8085  | Front UI (Thymeleaf)                            |
| `postgres`               | 5432  | БД с тремя схемами                              |
| `keycloak`               | 8180  | OAuth 2.0 Authorization Server                  |

## Архитектура

```
Browser ─► Front UI ─► Gateway ─► accounts / cash / transfer
                                    │          │         │
                                    └──► notifications ◄─┘

Все межсервисные вызовы идут ЧЕРЕЗ Eureka (lb://service-name)
и авторизуются по Client Credentials Flow (Keycloak).
Пользовательские вызовы от Front UI идут через Gateway с JWT
от Authorization Code Flow.
```

### Схема OAuth

| Клиент            | Flow                  | Scopes                                                             |
|-------------------|-----------------------|--------------------------------------------------------------------|
| `front-client`    | Authorization Code    | `accounts.read`, `accounts.write`, `cash.write`, `transfer.write`  |
| `gateway-client`  | Authorization Code    | `accounts.read`, `accounts.write`, `cash.write`, `transfer.write`  |
| `accounts-client` | Client Credentials    | `notifications.write`                                              |
| `cash-client`     | Client Credentials    | `accounts.write`, `notifications.write`                            |
| `transfer-client` | Client Credentials    | `accounts.write`, `notifications.write`                            |

### Схема БД

Одна база `bank`, три схемы (паттерн Database per Service на уровне схем):
- `accounts` — таблица `accounts` (id, login, name, birthdate, balance, version)
- `cash` — (будет в Этапе 3)
- `transfer` — (будет в Этапе 3)

## Запуск

### Предварительно
- Docker Desktop (или Docker Engine + Compose v2)
- JDK 21 и Maven 3.9+ — только для локального запуска без Docker

### Через Docker Compose (рекомендуется)

```bash
docker compose up -d --build
```

Порядок старта контролируется через `depends_on` + healthcheck:
1. Postgres (применяет `docker/init-db.sql`, создаёт схемы)
2. Keycloak (импортирует `keycloak/my-bank-realm.json`)
3. Eureka
4. Config Server (регистрируется в Eureka)
5. Accounts Service (читает конфиг из Config Server, регистрируется в Eureka)

### Проверка готовности

| Что проверить            | URL                                                                   |
|--------------------------|-----------------------------------------------------------------------|
| Eureka Dashboard         | http://localhost:8761                                                 |
| Config Server            | http://localhost:8888/accounts-service/default                        |
| Keycloak Admin Console   | http://localhost:8180 (admin/admin)                                   |
| Keycloak realm OpenID    | http://localhost:8180/realms/my-bank/.well-known/openid-configuration |
| Accounts — health        | http://localhost:8081/actuator/health                                 |

### Тестовые пользователи Keycloak

Все с паролем `password`:
- `ivan` — Иванов Иван (баланс 1000 руб)
- `petr` — Петров Пётр (баланс 500 руб)
- `sidor` — Сидоров Сидор (баланс 2000 руб)

### Проверка Gateway + Accounts end-to-end

После `docker compose up -d --build`:

1. Открыть в браузере **http://localhost:8080/api/accounts/me**
2. Gateway увидит неаутентифицированный запрос → редирект на Keycloak
3. Войти как `ivan` / `password`
4. Keycloak редиректит обратно → Gateway проксирует запрос в `accounts-service`
   с JWT в заголовке `Authorization` (TokenRelay)
5. Ответ: `{"login":"ivan","name":"Иванов Иван","birthdate":"1990-05-15","balance":1000.00}`

Также можно получить access token напрямую для curl-тестов:

```bash
TOKEN=$(curl -s -X POST \
  "http://localhost:8180/realms/my-bank/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=front-client" \
  -d "client_secret=front-secret" \
  -d "username=ivan" \
  -d "password=password" \
  -d "scope=accounts.read accounts.write" \
  | jq -r .access_token)

# Прямо в accounts-service (минуя Gateway):
curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/accounts/me
```

> Direct-grant (`password`) для `front-client` по умолчанию выключен. Если нужен
> для dev-проверок, включите в Keycloak Admin Console → Clients → front-client →
> "Direct access grants enabled: On". В продакшне этот флаг должен быть OFF.

## Локальный запуск (без Docker)

1. Поднять только инфраструктурные контейнеры:
   ```bash
   docker compose up -d postgres keycloak
   ```
2. Запустить Spring Boot модули по одному (в разных терминалах):
   ```bash
   mvn -pl eureka-server spring-boot:run
   mvn -pl config-server spring-boot:run
   mvn -pl accounts-service spring-boot:run
   ```

## Тестирование

```bash
# Все тесты всего мультипроекта
mvn clean verify

# Только unit-тесты accounts-service
mvn -pl accounts-service test

# Интеграционные тесты (требуют Docker для Testcontainers)
mvn -pl accounts-service failsafe:integration-test
```

Реализованы:
- **Unit**: `AccountServiceTest` — Mockito, проверка бизнес-правил
- **Integration**: `AccountControllerIT` — `@SpringBootTest` + Testcontainers PostgreSQL + MockMvc + mock JWT
- **Contract**: добавятся в Этапе 3 (Spring Cloud Contract между cash и accounts)

## Следующие этапы

- **Этап 2**: Gateway API со Spring Cloud Gateway MVC, проброс JWT в downstream
- **Этап 3**: Cash, Transfer, Notifications сервисы + контрактные тесты
- **Этап 4**: Front UI — замена `AccountStub` на `RestClient`, OAuth2 Login

## Troubleshooting

**`accounts-service` не может подключиться к Config Server при старте**
Убедитесь, что `config-server` полностью поднялся (может занять 30-60 сек).
В bootstrap.yml настроены retries — сервис попробует переподключиться.

**Keycloak выдаёт `Invalid token issuer`**
Обычная причина — `KC_HOSTNAME` в docker-compose не совпадает с
`KEYCLOAK_ISSUER_EXTERNAL` в переменных сервисов. Issuer в JWT
формируется Keycloak'ом по `KC_HOSTNAME`, и Resource Server валидирует
его строго по строке.
