{{/*
Secret holding sensitive environment variables for a microservice.
Same shape as the ConfigMap but goes into a Secret. Values are written
under stringData so Helm doesn't need to base64-encode them itself.

For real production use, secrets should come from an external secret
manager (Vault, External Secrets Operator, sealed-secrets etc). For
the practicum task, plain Helm values are sufficient.
*/}}
{{- define "common-microservice.secret" -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ .Values.secretName | default (printf "%s-secret" (include "common-microservice.name" .)) }}
  labels:
    {{- include "common-microservice.labels" . | nindent 4 }}
type: Opaque
stringData:
  {{- range $k, $v := .Values.secrets }}
  {{ $k }}: {{ $v | quote }}
  {{- end }}
{{- end -}}
