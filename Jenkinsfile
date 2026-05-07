// Пайплайн для банковского приложения. На каждом push'е
// собираем все 5 микросервисов, гоняем тесты, собираем docker-образы
// и разворачиваем в bank-test через Helm. Smoke-тесты (helm test)
// проверяют, что Ingress и actuator-эндпоинты живы.
//
// Требования к воркеру:
//   * JDK 21
//   * Maven 3.9+
//   * Docker (либо Rancher Desktop) — для сборки образов
//   * kubectl + helm (с настроенным kubeconfig до dev-кластера)
//
// Параметры:
//   IMAGE_TAG  — тег для docker-образов (по умолчанию build-${BUILD_NUMBER})
//   NAMESPACE  — namespace в Kubernetes (по умолчанию bank-test)
pipeline {
    agent any

    parameters {
        string(name: 'IMAGE_TAG', defaultValue: '', description: 'Docker image tag (default: build-${BUILD_NUMBER})')
        string(name: 'NAMESPACE', defaultValue: 'bank-test', description: 'Kubernetes namespace')
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
        IMAGE_TAG  = "${params.IMAGE_TAG ?: 'build-' + env.BUILD_NUMBER}"
        SERVICES   = 'accounts-service cash-service transfer-service notifications-service front'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean package'
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                script {
                    for (svc in env.SERVICES.split()) {
                        // У front модуль называется без -service-суффикса
                        def imageName = (svc == 'front') ? 'front' : svc
                        sh "docker build -f ./${svc}/Dockerfile -t bank/${imageName}:${IMAGE_TAG} ."
                    }
                }
            }
        }

        stage('Helm Lint') {
            steps {
                sh '''
                    for svc in accounts cash transfer notifications front-ui; do
                      (cd deploy/helm/$svc && helm dependency update)
                    done
                    (cd deploy/helm/bank && helm dependency update)
                    helm lint deploy/helm/bank \
                      --values deploy/helm/bank/values-test.yaml
                '''
            }
        }

        stage('Deploy to Kubernetes (test)') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-bank', variable: 'KUBECONFIG')]) {
                    sh '''
                        # Подменяем тег во всех values'ах через --set
                        helm upgrade --install bank ./deploy/helm/bank \
                          --namespace ${NAMESPACE} \
                          --create-namespace \
                          --values ./deploy/helm/bank/values-test.yaml \
                          --set "accounts.image.tag=${IMAGE_TAG}" \
                          --set "cash.image.tag=${IMAGE_TAG}" \
                          --set "transfer.image.tag=${IMAGE_TAG}" \
                          --set "notifications.image.tag=${IMAGE_TAG}" \
                          --set "front-ui.image.tag=${IMAGE_TAG}" \
                          --wait --timeout 5m
                    '''
                }
            }
        }

        stage('Helm Smoke Tests') {
            when {
                branch 'main'
            }
            steps {
                withCredentials([file(credentialsId: 'kubeconfig-bank', variable: 'KUBECONFIG')]) {
                    sh "helm test bank --namespace ${NAMESPACE} --logs"
                }
            }
        }
    }

    post {
        success {
            echo "Build #${env.BUILD_NUMBER} succeeded — image tag: ${IMAGE_TAG}"
        }
        failure {
            echo "Build #${env.BUILD_NUMBER} failed at stage: ${env.STAGE_NAME}"
        }
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}