{{/*
ConfigMap holding non-secret environment variables for a microservice.
Values.config is a flat map of UPPER_SNAKE_CASE keys → string values.
Spring Boot picks them up automatically (e.g. SPRING_DATASOURCE_URL
becomes spring.datasource.url at runtime).
*/}}
{{- define "common-microservice.configmap" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Values.configMapName | default (printf "%s-config" (include "common-microservice.name" .)) }}
  labels:
    {{- include "common-microservice.labels" . | nindent 4 }}
data:
  {{- range $k, $v := .Values.config }}
  {{ $k }}: {{ $v | quote }}
  {{- end }}
{{- end -}}
