{{/*
ClusterIP Service for a Spring Boot microservice. Exposes :8080 (http).
The service name is the chart name (e.g. "accounts"), so other services
can reach it as http://accounts:8080 via DNS.
*/}}
{{- define "common-microservice.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "common-microservice.name" . }}
  labels:
    {{- include "common-microservice.labels" . | nindent 4 }}
spec:
  type: {{ .Values.service.type | default "ClusterIP" }}
  ports:
    - name: http
      port: {{ .Values.service.port | default 8080 }}
      targetPort: http
      protocol: TCP
  selector:
    {{- include "common-microservice.selectorLabels" . | nindent 4 }}
{{- end -}}
