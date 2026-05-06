{{/*
Deployment template for a Spring Boot microservice.

The subchart calls this with `include "common-microservice.deployment" .`,
passing the full chart context. All inputs come from .Values:

  replicas              — pod replica count (default 1)
  port                  — container port (default 8080)
  image.repository      — image name
  image.tag             — image tag
  configMapName         — name of the ConfigMap to load envFrom (optional)
  secretName            — name of the Secret to load envFrom (optional)
  env                   — extra env vars as a map (optional)
  resources             — pod resource requests/limits (optional)
  hostAliases           — extra /etc/hosts entries on the pod (optional;
                          mainly used to make auth.bank.local resolve
                          inside the cluster to the ingress controller)
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
      containers:
        - name: {{ include "common-microservice.name" . }}
          image: {{ include "common-microservice.image" . | quote }}
          imagePullPolicy: {{ .Values.image.pullPolicy | default .Values.global.imagePullPolicy | default "IfNotPresent" }}
          ports:
            - name: http
              containerPort: {{ .Values.port | default 8080 }}
              protocol: TCP
          envFrom:
            {{- if .Values.configMapName }}
            - configMapRef:
                name: {{ .Values.configMapName }}
            {{- end }}
            {{- if .Values.secretName }}
            - secretRef:
                name: {{ .Values.secretName }}
            {{- end }}
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
