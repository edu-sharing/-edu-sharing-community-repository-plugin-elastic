{{- define "edusharing_repository_search_elastic_index.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_elastic_index.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "edusharing_repository_search_elastic_index.labels" -}}
{{ include "edusharing_repository_search_elastic_index.labels.instance" . }}
helm.sh/chart: {{ include "edusharing_repository_search_elastic_index.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "edusharing_repository_search_elastic_index.labels.instance" -}}
{{ include "edusharing_repository_search_elastic_index.labels.app" . }}
{{ include "edusharing_repository_search_elastic_index.labels.version" . }}
{{- end -}}

{{- define "edusharing_repository_search_elastic_index.labels.app" -}}
app: {{ include "edusharing_repository_search_elastic_index.name" . }}
app.kubernetes.io/name: {{ include "edusharing_repository_search_elastic_index.name" . }}
{{- end -}}

{{- define "edusharing_repository_search_elastic_index.labels.version" -}}
version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end -}}
