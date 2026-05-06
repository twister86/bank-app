{{/*
Deployment template for a Spring Boot microservice.

The subchart calls this with `include "common-microservice.deployment" .`,
passing the full chart context. All inputs come from .Values:

  replicas              — pod replica count (default 1)
  port                  — container port (default 8080)
  image.repository      — image name
  image.tag             — image tag
  image.pullPolicy      — pull policy (default IfNotPresent)
  configMapName         — Override ConfigMap name (auto: "<chart>-config")
  secretName            — Override Secret name (auto: "<chart>-secret")
  env                   — extra env vars as a map (optional)
  resources             — pod resource requests/limits (optional)
  hostAliases           — extra /etc/hosts entries on the pod (optional;
                          mainly used to make auth.bank.local resolve
                          inside the cluster to the ingress controller)
  waitForOidc           — URL of OIDC discovery endpoint to wait for
                          before main container starts. When set, an
                          initContainer polls the URL until it returns
                          200, so Spring Boot doesn't crash on
                          ClientRegistrations init while Keycloak is
                          still booting.
  probesEnabled         — enable readiness/liveness probes (default true)
  readinessPath         — readiness probe HTTP path
  livenessPath          — liveness probe HTTP path
  readinessInitialDelay — seconds before first readiness probe
  livenessInitialDelay  — seconds before first liveness probe
*/}}
{{- define "common-microservice.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ include "common-microservice.fullname" . }}
  labels:
    {{- include "common-microservice.labels" . | nindent 4 }}
spec:
  replicas: {{ .Values.replicas | default 1 }}
  selector:
    matchLabels:
      {{- include "common-microservice.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "common-microservice.selectorLabels" . | nindent 8 }}
      annotations:
        # Roll pods when ConfigMap or Secret content changes.
        # Without this, kubectl apply on a ConfigMap won't restart pods.
        checksum/config: {{ .Values | toYaml | sha256sum }}
    spec:
      {{- $hostAliases := .Values.hostAliases | default (.Values.global.hostAliases | default list) }}
      {{- if $hostAliases }}
      hostAliases:
        {{- toYaml $hostAliases | nindent 8 }}
      {{- end }}
      {{- if .Values.waitForOidc }}
      initContainers:
        - name: wait-for-oidc
          image: curlimages/curl:8.5.0
          command:
            - sh
            - -ec
            - |
              echo "Waiting for OIDC issuer to be available at $OIDC_URL..."
              for i in $(seq 1 120); do
                if curl -fsS "$OIDC_URL" >/dev/null 2>&1; then
                  echo "OIDC issuer is ready"
                  exit 0
                fi
                echo "Attempt $i: not ready, retrying in 3s"
                sleep 3
              done
              echo "OIDC issuer did not become ready in time"
              exit 1
          env:
            - name: OIDC_URL
              value: {{ .Values.waitForOidc | quote }}
      {{- end }}
      containers:
        - name: {{ include "common-microservice.name" . }}
          image: {{ include "common-microservice.image" . | quote }}
          imagePullPolicy: {{ .Values.image.pullPolicy | default .Values.global.imagePullPolicy | default "IfNotPresent" }}
          ports:
            - name: http
              containerPort: {{ .Values.port | default 8080 }}
              protocol: TCP
          envFrom:
            - configMapRef:
                name: {{ .Values.configMapName | default (printf "%s-config" (include "common-microservice.name" .)) }}
            - secretRef:
                name: {{ .Values.secretName | default (printf "%s-secret" (include "common-microservice.name" .)) }}
          {{- with .Values.env }}
          env:
            {{- range $k, $v := . }}
            - name: {{ $k }}
              value: {{ $v | quote }}
            {{- end }}
          {{- end }}
          {{- if .Values.probesEnabled | default true }}
          readinessProbe:
            httpGet:
              path: {{ .Values.readinessPath | default "/actuator/health/readiness" }}
              port: http
            initialDelaySeconds: {{ .Values.readinessInitialDelay | default 20 }}
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 6
          livenessProbe:
            httpGet:
              path: {{ .Values.livenessPath | default "/actuator/health/liveness" }}
              port: http
            initialDelaySeconds: {{ .Values.livenessInitialDelay | default 60 }}
            periodSeconds: 15
            timeoutSeconds: 3
            failureThreshold: 3
          {{- end }}
          {{- with .Values.resources }}
          resources:
            {{- toYaml . | nindent 12 }}
          {{- end }}
{{- end -}}