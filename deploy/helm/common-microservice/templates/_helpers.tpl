{{/*
Common helpers for the Bank application.

These helpers are designed to be included from subcharts. They derive
values from the subchart context (.Values, .Release, .Chart) and from
the umbrella chart's `global` map.

Usage in a subchart template:
  {{- include "common-microservice.fullname" . }}
*/}}

{{/*
Resolve the service name. Priority:
  1. .Values.nameOverride                    (explicit override)
  2. .Chart.Name                             (subchart directory name)
*/}}
{{- define "common-microservice.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Fullname = release-name plus chart name, unless fullnameOverride is set.
Used for resource names so that two releases in the same namespace don't
collide.
*/}}
{{- define "common-microservice.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Standard labels applied to every resource. Helm best-practice set
plus an `app` label for selectors.
*/}}
{{- define "common-microservice.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
app.kubernetes.io/name: {{ include "common-microservice.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: bank
app: {{ include "common-microservice.name" . }}
{{- end -}}

{{/*
Selector labels — must be a stable subset of the above (Deployment
selectors are immutable, so we never include version/chart here).
*/}}
{{- define "common-microservice.selectorLabels" -}}
app.kubernetes.io/name: {{ include "common-microservice.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app: {{ include "common-microservice.name" . }}
{{- end -}}

{{/*
Image reference. Pulls the registry from .Values.global.imageRegistry
when set, then the per-service .Values.image.repository and tag.
*/}}
{{- define "common-microservice.image" -}}
{{- $registry := .Values.global.imageRegistry | default "" -}}
{{- $repo := .Values.image.repository -}}
{{- $tag := .Values.image.tag | default .Chart.AppVersion -}}
{{- if $registry -}}
{{- printf "%s/%s:%s" $registry $repo $tag -}}
{{- else -}}
{{- printf "%s:%s" $repo $tag -}}
{{- end -}}
{{- end -}}
