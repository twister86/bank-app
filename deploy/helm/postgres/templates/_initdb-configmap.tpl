{{/*
Init scripts ConfigMap mounted at /docker-entrypoint-initdb.d in the
postgres container. The official postgres image runs everything in
this directory once on first boot (i.e. when PGDATA is empty).
Used to create per-service schemas before Liquibase runs.
*/}}
{{- define "postgres.initdb" -}}
{{- if .Values.postgres.initSql }}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "postgres.fullname" . }}-init
  labels:
    {{- include "postgres.labels" . | nindent 4 }}
data:
  init.sql: |
    {{- .Values.postgres.initSql | nindent 4 }}
{{- end }}
{{- end -}}