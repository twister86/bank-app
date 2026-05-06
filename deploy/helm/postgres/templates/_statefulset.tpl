{{/*
StatefulSet running a single PostgreSQL replica for one microservice.

If `.Values.postgres.initSql` is set, an init script ConfigMap is
mounted at /docker-entrypoint-initdb.d so the postgres image runs it
on first boot (used to create per-service schemas before Liquibase).
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
                  name: {{ .Values.secretName | default (printf "%s-secret" (include "common-microservice.name" .)) }}
                  key: SPRING_DATASOURCE_PASSWORD
            - name: PGDATA
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
            {{- if .Values.postgres.initSql }}
            - name: init-scripts
              mountPath: /docker-entrypoint-initdb.d
              readOnly: true
            {{- end }}
          {{- with .Values.postgres.resources }}
          resources:
            {{- toYaml . | nindent 12 }}
          {{- end }}
      {{- if .Values.postgres.initSql }}
      volumes:
        - name: init-scripts
          configMap:
            name: {{ include "postgres.fullname" . }}-init
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