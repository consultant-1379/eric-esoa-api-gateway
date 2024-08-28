{{/*
Expand the name of the chart.
*/}}
{{- define "eric-esoa-api-gateway.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create release name used for cluster role.
*/}}
{{- define "eric-esoa-api-gateway.release.name" -}}
{{- default .Release.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-esoa-api-gateway.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create user-defined annotations
*/}}
{{ define "eric-esoa-api-gateway.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-esoa-api-gateway.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}

{{/*
 create prometheus info
*/}}
{{- define "eric-esoa-api-gateway.prometheus" -}}
prometheus.io/path: "{{ .Values.prometheus.path }}"
prometheus.io/port: "{{ .Values.prometheus.port }}"
prometheus.io/scrape: "{{ .Values.prometheus.scrape }}"
{{- end -}}
{{/*
Create user-defined labels
*/}}
{{ define "eric-esoa-api-gateway.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-esoa-api-gateway.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) }}
{{- end }}
{{/*
Merge eric-product-info, and user-defined annotations into a single set
of metadata annotations.
*/}}
{{- define "eric-esoa-api-gateway.annotations" -}}
  {{- $productInfo := include "eric-esoa-api-gateway.eric-product-info" . | fromYaml -}}
  {{- $config := include "eric-esoa-api-gateway.config-annotations" . | fromYaml -}}
  {{- include "eric-esoa-api-gateway.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
{{- end -}}

{{/*
Merge kubernetes-io-info, user-defined labels, and app and chart labels into a single set
of metadata labels.
*/}}
{{- define "eric-esoa-api-gateway.labels" -}}
  {{- $kubernetesIoInfo := include "eric-esoa-api-gateway.kubernetes-io-info" . | fromYaml -}}
  {{- $config := include "eric-esoa-api-gateway.config-labels" . | fromYaml -}}
  {{- include "eric-esoa-api-gateway.mergeLabels" (dict "location" .Template.Name "sources" (list $kubernetesIoInfo $config)) | trim }}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "eric-esoa-api-gateway.common-labels" }}
app.kubernetes.io/name: {{ .Chart.Name | quote }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
app.kubernetes.io/version: {{ .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" | quote }}
{{- end -}}

{{/*
Create Ericsson product app.kubernetes.io info
*/}}
{{- define "eric-esoa-api-gateway.kubernetes-io-info" -}}
helm.sh/chart: {{ include "eric-esoa-api-gateway.chart" . }}
{{- include "eric-esoa-api-gateway.common-labels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Create app and chart metadata labels
*/}}
{{- define "eric-esoa-api-gateway.app-and-chart-labels" -}}
app: {{ template "eric-esoa-api-gateway.name" . }}
chart: {{ template "eric-esoa-api-gateway.chart" . }}
{{- end -}}

{{/*
Create Ericsson Product Info
*/}}
{{- define "eric-esoa-api-gateway.eric-product-info" -}}
ericsson.com/product-name: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productName | quote }}
ericsson.com/product-number: {{ (fromYaml (.Files.Get "eric-product-info.yaml")).productNumber | quote }}
ericsson.com/product-revision: {{ regexReplaceAll "(.*)[+|-].*" .Chart.Version "${1}" | quote }}
{{- end}}

{{/*
Create keycloak-client/API Gateway image pull secrets
*/}}
{{- define "eric-esoa-api-gateway.image.pullSecrets" -}}
    {{- if .Values.imageCredentials -}}
        {{- if .Values.imageCredentials.pullSecret -}}
            {{- print .Values.imageCredentials.pullSecret -}}
        {{- else if .Values.global.pullSecret -}}
            {{- print .Values.global.pullSecret -}}
        {{- end -}}
    {{- else if .Values.global.pullSecret -}}
        {{- print .Values.global.pullSecret -}}
    {{- else if .Values.imageCredentials.registry -}}
        {{- if .Values.imageCredentials.registry.pullSecret -}}
            {{- print .Values.imageCredentials.registry.pullSecret -}}
        {{- else if .Values.global.registry.pullSecret -}}
            {{- print .Values.global.registry.pullSecret -}}
        {{- end -}}
    {{- else -}}
            {{- print .Values.global.registry.pullSecret -}}
    {{- end -}}
{{- end -}}

{{/*
IDUN-1719 : DR-D1123-124
*/}}
{{- define "eric-esoa-api-gateway.securityPolicy.reference" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.security -}}
      {{- if .Values.global.security.policyReferenceMap -}}
        {{ $mapped := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" }}
        {{- if $mapped -}}
          {{ $mapped }}
        {{- else -}}
          {{- default "default-restricted-security-policy" .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
        {{- end -}}
      {{- else -}}
        {{- default "default-restricted-security-policy" .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
      {{- end -}}
    {{- else -}}
      {{- default "default-restricted-security-policy" .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
    {{- end -}}
  {{- else -}}
    {{- default "default-restricted-security-policy" .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
  {{- end -}}
{{- end -}}

{{/*
IDUN-1719 : DR-D1123-124
*/}}
{{- define "eric-esoa-api-gateway.securityPolicy.annotations" }}
ericsson.com/security-policy.name: "restricted/default"
ericsson.com/security-policy.privileged: "false"
ericsson.com/security-policy.capabilities: "N/A"
{{- end -}}

{{/*
The name of the cluster role used during openshift deployments.
This helper is provided to allow use of the new global.security.privilegedPolicyClusterRoleName if set, otherwise
use the previous naming convention of <release_name>-allowed-use-privileged-policy for backwards compatibility.
*/}}
{{- define "eric-esoa-api-gateway.privileged.cluster.role.name" -}}
  {{- if hasKey (.Values.global.security) "privilegedPolicyClusterRoleName" -}}
    {{ .Values.global.security.privilegedPolicyClusterRoleName }}
  {{- else -}}
    {{ template "eric-esoa-api-gateway.release.name" . }}-allowed-use-privileged-policy
  {{- end -}}
{{- end -}}

{{/*
IDUN-11334: DR-D470217-007-AD
This helper defines whether this service enter the Service Mesh or not.
It enters only if the global switch is enabled (default false)
and the local switch is enabled (default true)
*/}}
{{- define "eric-esoa-api-gateway.service-mesh-enabled" }}
  {{- $globalMeshEnabled := "false" -}}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
        {{- $globalMeshEnabled = .Values.global.serviceMesh.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $globalMeshEnabled -}}
{{- end -}}

{{/*
IDUN-11334: DR-D470217-011
This helper defines the annotation which bring the service into the mesh.
*/}}
{{- define "eric-esoa-api-gateway.service-mesh-inject" }}
{{- if eq (include "eric-esoa-api-gateway.service-mesh-enabled" .) "true" }}
sidecar.istio.io/inject: "true"
sidecar.istio.io/proxyMemoryLimit: 2Gi
{{- else }}
sidecar.istio.io/inject: "false"
{{- end }}
{{- end -}}

{{/*
This helper defines which out-mesh services will be reached by this one.
*/}}
{{- define "eric-esoa-api-gateway.service-mesh-ism2osm-labels" }}
{{- if eq (include "eric-esoa-api-gateway.service-mesh-enabled" .) "true" }}
  {{- if eq (include "eric-esoa-api-gateway.global-security-tls-enabled" .) "true" }}
eric-ctrl-bro-ism-access: "true"
eric-cnom-server-ism-access: "true"
eric-pm-server-ism-access: "true"
eric-adp-gui-aggregator-service-ism-access: "true"
eric-sec-certm-ism-access: "true"
  {{- end }}
{{- end -}}
{{- end -}}


{{/*
IDUN-11334: GL-D470217-080-AD
This helper captures the service mesh version from the integration chart to
annotate the workloads so they are redeployed in case of service mesh upgrade.
*/}}
{{- define "eric-esoa-api-gateway.service-mesh-version" }}
{{- if eq (include "eric-esoa-api-gateway.service-mesh-enabled" .) "true" }}
  {{- if .Values.global -}}
    {{- if .Values.global.serviceMesh -}}
      {{- if .Values.global.serviceMesh.annotations -}}
        {{- .Values.global.serviceMesh.annotations | toYaml -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
check global.security.tls.enabled since it is removed from values.yaml
*/}}
{{- define "eric-esoa-api-gateway.global-security-tls-enabled" -}}
{{- if  .Values.global -}}
  {{- if  .Values.global.security -}}
    {{- if  .Values.global.security.tls -}}
       {{- .Values.global.security.tls.enabled | toString -}}
    {{- else -}}
       {{- "false" -}}
    {{- end -}}
  {{- else -}}
       {{- "false" -}}
  {{- end -}}
{{- else -}}
{{- "false" -}}
{{- end -}}
{{- end -}}

{{/*
This helper defines the annotation for define service mesh volume
*/}}
{{- define "eric-esoa-api-gateway.service-mesh-volume" }}
{{- if and (eq (include "eric-esoa-api-gateway.service-mesh-enabled" .) "true") (eq (include "eric-esoa-api-gateway.global-security-tls-enabled" .) "true") }}
sidecar.istio.io/userVolume: '{"api-gateway-certs-pm-tls":{"secret":{"secretName":"eric-esoa-api-gateway-siptls-pm-secret","optional":true}},"api-gateway-certs-cnom-tls":{"secret":{"secretName":"eric-esoa-api-gateway-siptls-cnom-secret","optional":true}},"api-gateway-certs-gas-tls":{"secret":{"secretName":"eric-esoa-api-gateway-siptls-gas-secret","optional":true}},"api-gateway-certs-certm-tls":{"secret":{"secretName":"eric-esoa-api-gateway-siptls-certm-secret","optional":true}},"api-gateway-certs-bro-tls":{"secret":{"secretName":"eric-esoa-api-gateway-siptls-bro-secret","optional":true}},"api-gateway-certs-ca-tls":{"secret":{"secretName":"eric-sec-sip-tls-trusted-root-cert"}}}'
sidecar.istio.io/userVolumeMount: '{"api-gateway-certs-pm-tls":{"mountPath":"/etc/istio/tls/eric-pm-server/","readOnly":true},"api-gateway-certs-cnom-tls":{"mountPath":"/etc/istio/tls/eric-cnom-server/","readOnly":true},"api-gateway-certs-certm-tls":{"mountPath":"/etc/istio/tls/eric-sec-certm/","readOnly":true},"api-gateway-certs-bro-tls":{"mountPath":"/etc/istio/tls/eric-ctrl-bro/","readOnly":true},"api-gateway-certs-gas-tls":{"mountPath":"/etc/istio/tls/eric-adp-gui-aggregator-service-http/","readOnly":true},"api-gateway-certs-ca-tls":{"mountPath":"/etc/istio/tls-ca","readOnly":true}}'
{{ end }}
{{- end -}}

{{- /*
Wrapper functions to set the contexts
*/ -}}
{{- define "eric-esoa-api-gateway.mergeAnnotations" -}}
  {{- include "eric-esoa-api-gateway.aggregatedMerge" (dict "context" "annotations" "location" .location "sources" .sources) }}
{{- end -}}
{{- define "eric-esoa-api-gateway.mergeLabels" -}}
  {{- include "eric-esoa-api-gateway.aggregatedMerge" (dict "context" "labels" "location" .location "sources" .sources) }}
{{- end -}}

{{- /*
Generic function for merging annotations and labels (version: 1.0.1)
{
    context: string
    sources: [[sourceData: {key => value}]]
}
This generic merge function is added to improve user experience
and help ADP services comply with the following design rules:
  - DR-D1121-060 (global labels and annotations)
  - DR-D1121-065 (annotations can be attached by application
                  developers, or by deployment engineers)
  - DR-D1121-068 (labels can be attached by application
                  developers, or by deployment engineers)
  - DR-D1121-160 (strings used as parameter value shall always
                  be quoted)
Installation or template generation of the Helm chart fails when:
  - same key is listed multiple times with different values
  - when the input is not string
IMPORTANT: This function is distributed between services verbatim.
Fixes and updates to this function will require services to reapply
this function to their codebase. Until usage of library charts is
supported in ADP, we will keep the function hardcoded here.
*/ -}}
{{- define "eric-esoa-api-gateway.aggregatedMerge" -}}
  {{- $merged := dict -}}
  {{- $context := .context -}}
  {{- $location := .location -}}
  {{- range $sourceData := .sources -}}
    {{- range $key, $value := $sourceData -}}
      {{- /* FAIL: when the input is not string. */ -}}
      {{- if not (kindIs "string" $value) -}}
        {{- $problem := printf "Failed to merge keys for \"%s\" in \"%s\": invalid type" $context $location -}}
        {{- $details := printf "in \"%s\": \"%s\"." $key $value -}}
        {{- $reason := printf "The merge function only accepts strings as input." -}}
        {{- $solution := "To proceed, please pass the value as a string and try again." -}}
        {{- printf "%s %s %s %s" $problem $details $reason $solution | fail -}}
      {{- end -}}
      {{- if hasKey $merged $key -}}
        {{- $mergedValue := index $merged $key -}}
        {{- /* FAIL: when there are different values for a key. */ -}}
        {{- if ne $mergedValue $value -}}
          {{- $problem := printf "Failed to merge keys for \"%s\" in \"%s\": key duplication in" $context $location -}}
          {{- $details := printf "\"%s\": (\"%s\", \"%s\")." $key $mergedValue $value -}}
          {{- $reason := printf "The same key cannot have different values." -}}
          {{- $solution := "To proceed, please resolve the conflict and try again." -}}
          {{- printf "%s %s %s %s" $problem $details $reason $solution | fail -}}
        {{- end -}}
      {{- end -}}
      {{- $_ := set $merged $key $value -}}
    {{- end -}}
  {{- end -}}
{{- /*
Strings used as parameter value shall always be quoted. (DR-D1121-160)
The below is a workaround to toYaml, which removes the quotes.
Instead we loop over and quote each value.
*/ -}}
{{- range $key, $value := $merged }}
{{ $key }}: {{ $value | quote }}
{{- end -}}
{{- end -}}

{{/*
This helper defines whether DST is enabled or not.
*/}}
{{- define "eric-esoa-api-gateway.dst-enabled" }}
  {{- $dstEnabled := "false" -}}
  {{- if .Values.dst -}}
    {{- if .Values.dst.enabled -}}
        {{- $dstEnabled = .Values.dst.enabled -}}
    {{- end -}}
  {{- end -}}
  {{- $dstEnabled -}}
{{- end -}}

{{/*
Define the labels needed for DST
*/}}
{{- define "eric-esoa-api-gateway.dstLabels" -}}
{{- if eq (include "eric-esoa-api-gateway.dst-enabled" .) "true" }}
eric-dst-collector-access: "true"
{{- end }}
{{- end -}}

{{/*
Define the annotations needed for DST
*/}}
{{- define "eric-esoa-api-gateway.dstAnnotations" }}
{{- if and (eq (include "eric-esoa-api-gateway.service-mesh-enabled" .) "true") (eq (include "eric-esoa-api-gateway.dst-enabled" .) "true") }}
traffic.sidecar.istio.io/excludeOutboundPorts: {{ .Values.dst.collector.portZipkinHttp }}
{{- end }}
{{- end -}}


{{/*
Define DST environment variables
*/}}
{{ define "eric-esoa-api-gateway.dstEnv" }}
{{- if eq (include "eric-esoa-api-gateway.dst-enabled" .) "true" }}
- name: SPRING_SLEUTH_ENABLED
  value: "true"
- name: SPRING_ZIPKIN_ENABLED
  value: "true"
- name: SPRING_ZIPKIN_SERVER
  value: {{ .Values.dst.collector.host }}:{{ .Values.dst.collector.portZipkinHttp }}
{{- else }}
- name: SPRING_SLEUTH_ENABLED
  value: "false"
- name: SPRING_ZIPKIN_ENABLED
  value: "false"
{{- end -}}
{{ end }}
