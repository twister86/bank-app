{{/*
Resolve the database hostname. By convention each microservice gets
a database named "<service>-db" so the JDBC URL inside the cluster
becomes jdbc:postgresql://<service>-db:5432/<database>.
*/}}
{{- define "postgres.fullname" -}}
{{- printf "%s-db" (include "common-microservice.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "postgres.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/name: {{ include "postgres.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: database
app.kubernetes.io/part-of: bank
app.kubernetes.io/managed-by: {{ .Release.Service }}
app: {{ include "postgres.fullname" . }}
{{- end -}}

{{- define "postgres.selectorLabels" -}}
app.kubernetes.io/name: {{ include "postgres.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app: {{ include "postgres.fullname" . }}
{{- end -}}
