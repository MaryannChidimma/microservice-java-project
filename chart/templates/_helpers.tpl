{{/* Standard labels for every object in the chart */}}
{{- define "depot.labels" -}}
app.kubernetes.io/name: depot
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}