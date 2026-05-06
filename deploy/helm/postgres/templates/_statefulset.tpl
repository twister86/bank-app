{{/*
StatefulSet running a single PostgreSQL replica for one microservice.

We deliberately use a per-service database (rather than one shared DB
with multiple schemas) so each microservice can be scaled, backed up
and migrated independently — the same boundary that exists between
the services at the code level.

The volumeClaimTemplate provisions storage from the default
StorageClass. On Minikube that's `standard`, backed by hostPath.
*/}}
{{- define "postgres.statefulset" -}}
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: {{ include "postgres.fullname" . }}
  labels:
    {{- include "postgres.labels" . | nindent 4 }}
spec:
  serviceName: {{ include "postgres.fullname" . }}
  replicas: 1
  selector:
    matchLabels:
      {{- include "postgres.selectorLabels" . | nindent 6 }}
  template:
    metadata:
      labels:
        {{- include "postgres.selectorLabels" . | nindent 8 }}
    spec:
      containers:
        - name: postgres
          image: "{{ .Values.postgres.image | default "postgres:16-alpine" }}"
          imagePullPolicy: IfNotPresent
          ports:
            - name: postgres
              containerPort: 5432
          env:
            - name: POSTGRES_DB
              value: {{ .Values.postgres.database | quote }}
            - name: POSTGRES_USER
              value: {{ .Values.postgres.username | quote }}
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  # Reuse the application's Secret so the password is
                  # defined exactly once in values.yaml.
                  name: {{ .Values.secretName | default (printf "%s-secret" (include "common-microservice.name" .)) }}
                  key: SPRING_DATASOURCE_PASSWORD
            - name: PGDATA
              # Subdirectory inside the mounted volume — required when
              # the volume root contains lost+found etc. Without it,
              # initdb refuses to run on a non-empty directory.
              value: /var/lib/postgresql/data/pgdata
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "{{ .Values.postgres.username }}", "-d", "{{ .Values.postgres.database }}"]
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
          livenessProbe:
            exec:
              command: ["pg_isready", "-U", "{{ .Values.postgres.username }}", "-d", "{{ .Values.postgres.database }}"]
            initialDelaySeconds: 30
            periodSeconds: 15
          volumeMounts:
            - name: data
              mountPath: /var/lib/postgresql/data
          {{- with .Values.postgres.resources }}
          resources:
            {{- toYaml . | nindent 12 }}
          {{- end }}
  volumeClaimTemplates:
    - metadata:
        name: data
      spec:
        accessModes: ["ReadWriteOnce"]
        {{- with .Values.postgres.storageClassName }}
        storageClassName: {{ . }}
        {{- end }}
        resources:
          requests:
            storage: {{ .Values.postgres.storage | default "1Gi" }}
{{- end -}}
