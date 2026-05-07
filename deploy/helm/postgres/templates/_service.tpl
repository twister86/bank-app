{{/*
Headless Service that fronts the StatefulSet. clusterIP: None means
DNS returns the pod IPs directly, which is the standard pattern for
StatefulSets — each pod gets a stable DNS name (<service>-0,
<service>-1 …) within the headless service.

For a single-replica DB this is essentially equivalent to a normal
ClusterIP, but using the headless form keeps the door open to add
read replicas later without changing service topology.
*/}}
{{- define "postgres.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ include "postgres.fullname" . }}
  labels:
    {{- include "postgres.labels" . | nindent 4 }}
spec:
  clusterIP: None
  ports:
    - name: postgres
      port: 5432
      targetPort: postgres
  selector:
    {{- include "postgres.selectorLabels" . | nindent 4 }}
{{- end -}}
