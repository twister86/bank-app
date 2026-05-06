# Bank — Sprint 10 (Kubernetes + Helm)

Микросервисное приложение «Банк» развёрнутое в Kubernetes через Helm-чарты.
Это Sprint 10: переход с инфраструктурного стека Sprint 9 (Spring Cloud
Gateway + Eureka + Spring Cloud Config + docker-compose) на штатные
средства Kubernetes (Ingress + DNS + ConfigMap/Secret).

Локальная среда: **Rancher Desktop 1.22** (k3s под капотом) +
**ingress-nginx** (Traefik отключён).

## Состав приложения

| Компонент         | Технология                     | Где развёрнут                         |
|-------------------|--------------------------------|---------------------------------------|
| Front UI          | Spring Boot + Thymeleaf        | внутри K8s, Helm-сабчарт `front-ui`   |
| Accounts          | Spring Boot + JPA + PostgreSQL | внутри K8s, сабчарт `accounts`        |
| Cash              | Spring Boot                    | внутри K8s, сабчарт `cash`            |
| Transfer          | Spring Boot                    | внутри K8s, сабчарт `transfer`        |
| Notifications     | Spring Boot                    | внутри K8s, сабчарт `notifications`   |
| Auth Server       | Keycloak (Bitnami chart)       | внутри K8s, dependency umbrella-чарта |
| Service discovery | Kubernetes DNS                 | вместо Eureka                         |
| Config            | ConfigMap / Secret             | вместо Spring Cloud Config            |
| Gateway API       | Ingress (ingress-nginx)        | вместо Spring Cloud Gateway           |

## Структура Helm-чартов

```
deploy/helm/
├── bank/                       umbrella chart (entry point)
│   ├── Chart.yaml              dependencies на все сабчарты + Bitnami Keycloak
│   ├── values.yaml             общие defaults
│   ├── values-dev.yaml         overrides для bank-dev namespace
│   ├── values-test.yaml        overrides для bank-test
│   ├── values-prod.yaml        overrides для bank-prod
│   └── templates/
│       ├── ingress.yaml        единый Ingress: /api/* → backend, / → front
│       └── tests/test-ingress.yaml
│
├── common-microservice/        library chart с шаблонами Deployment/Service/CM/Secret
├── postgres/                   library chart с StatefulSet + headless Service
│
├── accounts/                   subchart микросервиса аккаунтов
├── cash/                       subchart Cash
├── transfer/                   subchart Transfer
├── notifications/              subchart Notifications (без БД)
├── front-ui/                   subchart фронта
└── keycloak-realm/             subchart с ConfigMap для импорта realm в Keycloak
    ├── realms/                 общий placeholder
    ├── realms-dev/             realm для bank-dev (импортируется в Keycloak)
    ├── realms-test/            realm для bank-test
    └── realms-prod/            realm для bank-prod
```

### Зачем library charts

`common-microservice` и `postgres` — это library charts (`type: library`
в их `Chart.yaml`). Они не устанавливаются сами по себе, а предоставляют
шаблоны, которые подключают сабчарты микросервисов через
`{{- include "common-microservice.deployment" . }}`. Это убирает 80%
дублирования между четырьмя похожими микросервисами: всё, что
отличается у `accounts/cash/transfer/notifications` — это значения в
`values.yaml`, а сами шаблоны общие.

## Подготовка Rancher Desktop

### 1. Установка

Скачать и установить **Rancher Desktop 1.22** с
[github.com/rancher-sandbox/rancher-desktop/releases/tag/v1.22.0](https://github.com/rancher-sandbox/rancher-desktop/releases/tag/v1.22.0).

### 2. Проверить, что kubectl смотрит в правильный кластер

```bash
kubectl config use-context rancher-desktop
kubectl get nodes
# NAME                   STATUS   ROLES                  AGE   VERSION
# lima-rancher-desktop   Ready    control-plane,master   2m    v1.30.x+k3s1
```

### 3. Поставить ingress-nginx

```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

helm upgrade --install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --create-namespace \
  --wait

# Дождаться готовности
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s
```

### 4. Прописать hostnames

```bash
# macOS / Linux:
echo "127.0.0.1 bank.dev.local auth.bank.dev.local" | sudo tee -a /etc/hosts

# Windows: добавить ту же строку в C:\Windows\System32\drivers\etc\hosts
# (запустить блокнот от имени администратора)
```

### 5. Резолв `auth.bank.dev.local` ВНУТРИ кластера

Микросервисы валидируют JWT, обращаясь к Keycloak по `issuer-uri`,
который должен совпадать с тем, что видит браузер (иначе claim `iss`
в токене не пройдёт валидацию). Это типичная боль OAuth в K8s.

Внутри кластера CoreDNS не знает, как резолвить `auth.bank.dev.local`,
поэтому мы пробрасываем эту запись через `hostAliases` прямо в
`/etc/hosts` каждого пода. IP — это ClusterIP сервиса
ingress-nginx-controller:

```bash
INGRESS_IP=$(kubectl -n ingress-nginx get svc ingress-nginx-controller \
  -o jsonpath='{.spec.clusterIP}')
echo "Use this IP in values:  $INGRESS_IP"
```

Подставить полученный IP в `deploy/helm/bank/values.yaml` под
`global.hostAliases[0].ip` (или передать через `--set` при установке —
см. раздел «Развёртывание»).

Альтернатива — настроить CoreDNS rewrite на уровне кластера; чище,
но требует правки `coredns` ConfigMap в `kube-system`.

## Подготовка realm-файла Keycloak

Один realm на окружение. Положить экспорт реалма (как
`keycloak/my-bank-realm.json` из Sprint 9) в нужную подпапку:

```
deploy/helm/keycloak-realm/realms-dev/bank-dev-realm.json
deploy/helm/keycloak-realm/realms-test/bank-test-realm.json
deploy/helm/keycloak-realm/realms-prod/bank-prod-realm.json
```

Имя realm внутри JSON должно совпадать с тем, что прописано в `issuer-uri`
в `values-<env>.yaml` (`bank-dev` / `bank-test` / `bank-prod`).

В realm должны быть клиенты:
- `bank-front` — public client, grant type `authorization_code`, redirect URI
  `http://bank.dev.local/login/oauth2/code/keycloak` (и аналоги для test/prod).
- `accounts-service`, `cash-service`, `transfer-service` — confidential clients,
  grant type `client_credentials`.
- Скоупы: `accounts.read`, `accounts.write`, `cash.write`, `transfer.write`,
  `notifications.write`.

## Сборка образов

С Rancher Desktop в режиме `dockerd (moby)` локальный `docker` уже
указывает на K8s daemon — собранные образы видны в кластере без
дополнительных настроек.

```bash
mvn -B clean package -DskipTests

docker build -t bank/accounts-service:1.1.0       ./accounts-service
docker build -t bank/cash-service:1.1.0           ./cash-service
docker build -t bank/transfer-service:1.1.0       ./transfer-service
docker build -t bank/notifications-service:1.1.0  ./notifications-service
docker build -t bank/front:1.1.0                  ./front
```

В сабчартах `image.pullPolicy: IfNotPresent` (см. `values.yaml`) —
Kubernetes не будет пытаться скачать образ из registry, найдёт
локальный.

> **Если используется `containerd`-engine**, замени `docker build` на
> `nerdctl --namespace k8s.io build` — образы должны попасть именно в
> namespace `k8s.io`, иначе k3s их не увидит.

## Развёртывание

```bash
cd deploy/helm/bank

# Подтянуть transitive dependencies (Bitnami Keycloak с charts.bitnami.com)
helm dependency update

# Узнать IP ingress controller для hostAliases
INGRESS_IP=$(kubectl -n ingress-nginx get svc ingress-nginx-controller \
  -o jsonpath='{.spec.clusterIP}')

# Создать namespace и поставить релиз
kubectl create namespace bank-dev
helm install bank . \
  --namespace bank-dev \
  --values values-dev.yaml \
  --set "global.hostAliases[0].ip=${INGRESS_IP}" \
  --set "global.hostAliases[0].hostnames[0]=auth.bank.dev.local" \
  --wait --timeout 5m

# Проверить состояние
kubectl -n bank-dev get pods,svc,ingress,statefulsets,configmaps,secrets
```

Открыть в браузере: `http://bank.dev.local`. Откроется страница входа
Keycloak; после успешного входа — главная страница фронта.

### Развёртывание отдельного микросервиса

Любой сабчарт можно поставить независимо (например, для разработки
или CI):

```bash
cd deploy/helm/accounts
helm dependency update
helm install accounts . --namespace bank-dev --create-namespace
```

## Helm-тесты

В каждом сабчарте есть pod с аннотацией `helm.sh/hook: test`, который
проверяет `/actuator/health/readiness` соответствующего сервиса. На
уровне umbrella есть тест, проверяющий доступность Front через Ingress.

```bash
helm test bank --namespace bank-dev
```

Ожидаемый вывод:
```
TEST SUITE:     bank-accounts-test           Phase: Succeeded
TEST SUITE:     bank-cash-test               Phase: Succeeded
TEST SUITE:     bank-transfer-test           Phase: Succeeded
TEST SUITE:     bank-notifications-test      Phase: Succeeded
TEST SUITE:     bank-front-ui-test           Phase: Succeeded
TEST SUITE:     bank-ingress-test            Phase: Succeeded
```

Если какой-то pod упал — `kubectl logs <pod> -n bank-dev` покажет, что
именно не отвечает.

## Развёртывание в test и prod

```bash
# test
kubectl create namespace bank-test
helm install bank . -n bank-test -f values-test.yaml \
  --set "global.hostAliases[0].ip=${INGRESS_IP}" \
  --set "global.hostAliases[0].hostnames[0]=auth.bank.test.local" \
  --wait

# prod
kubectl create namespace bank-prod
helm install bank . -n bank-prod -f values-prod.yaml \
  --set "global.hostAliases[0].ip=${INGRESS_IP}" \
  --set "global.hostAliases[0].hostnames[0]=auth.bank.prod.local" \
  --wait
```

`values-prod.yaml` поднимает реплики до 2, добавляет resource
requests/limits, увеличивает PVC до 10Gi, переключает Keycloak на
HTTPS issuer-uri.

В `/etc/hosts` нужно добавить hostnames для каждого окружения
(`bank.test.local`, `bank.prod.local` и их `auth.*` варианты).

## Удаление

```bash
helm uninstall bank --namespace bank-dev
# StatefulSet PVC по умолчанию НЕ удаляются — это спасает данные
# при случайном `helm uninstall`. Чтобы стереть и их:
kubectl -n bank-dev delete pvc --all
```

## Что было удалено из Sprint 9

- модули `config-server`, `eureka-server`, `gateway` — выпилены целиком
- `bootstrap.yml` в каждом сервисе — заменён на обычный `application.yml`
- зависимости `spring-cloud-starter-config`, `spring-cloud-starter-bootstrap`,
  `spring-cloud-starter-netflix-eureka-client` — удалены из pom.xml каждого
  сервиса
- `docker-compose.yml` остался для локальной разработки без K8s, но больше
  не запускает Eureka/Config Server

## Локальная разработка без Kubernetes

`application.yml` каждого сервиса написан так, что значения берутся
из env-переменных с дефолтами на `localhost`. То есть запустить
сервис из IDE можно, подняв только PostgreSQL и Keycloak:

```bash
docker compose up -d postgres keycloak
mvn -pl accounts-service spring-boot:run
```

## Troubleshooting

**Pod долго не поднимается, в логах `Connection refused: auth.bank.dev.local`**
→ `hostAliases` указывает на неправильный IP. Перезапусти helm install с
актуальным `${INGRESS_IP}`.

**Helm test `bank-ingress-test` падает с `Could not resolve host`**
→ Проверь, что ingress-nginx стоит в namespace `ingress-nginx` (значение по
умолчанию). Если в другом — переопредели `umbrella.ingressTest.targetService`
в values.

**`helm dependency update` падает на Bitnami repo**
→ Добавь репозиторий явно:
```bash
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
```

**Браузер не открывает `bank.dev.local`**
→ Проверь `/etc/hosts` (`127.0.0.1 bank.dev.local`) и что
ingress-nginx-controller на `127.0.0.1:80` отвечает:
```bash
curl -I http://127.0.0.1
# HTTP/1.1 404 Not Found  ← это нормально, ingress есть, но не знает hostname
curl -I http://bank.dev.local
# HTTP/1.1 302 Found  ← правильный ответ (редирект на Keycloak login)
```

**Pod падает с `liquibase: ERROR: relation "databasechangelog" already exists`**
→ Удалил релиз без удаления PVC, потом поставил заново. Состояние БД
осталось от прошлого релиза. Либо `kubectl delete pvc --all -n bank-dev`,
либо мигрируй вручную, либо переключи `SPRING_LIQUIBASE_DROP_FIRST=true`
один раз в values.

**Rancher Desktop не отдаёт 80 порт на Linux**
→ На Linux non-root юзер не может слушать порты <1024. Выполни
`sudo sysctl -w net.ipv4.ip_unprivileged_port_start=80` и пропиши
эту строку в `/etc/sysctl.conf` для постоянного эффекта.
