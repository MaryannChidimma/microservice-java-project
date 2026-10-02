#!/usr/bin/env bash
# Install Knative Serving + Kourier on the existing Minikube cluster.
# Safe to run again: every step is a 'kubectl apply' or a patch.
#
# default-domain is skipped on purpose: it waits for an external IP that
# Minikube only provides with 'minikube tunnel'. Functions are reached with
#   kubectl port-forward -n kourier-system svc/kourier 8091:80
# plus a Host header (see scripts/coldstart.sh).
set -euo pipefail

KNATIVE_VERSION=knative-v1.23.0
SERVING=https://github.com/knative/serving/releases/download/$KNATIVE_VERSION
KOURIER=https://github.com/knative-extensions/net-kourier/releases/download/$KNATIVE_VERSION

echo "==> Knative Serving CRDs"
kubectl apply -f "$SERVING/serving-crds.yaml"
kubectl wait --for=condition=Established crd --all --timeout=60s

echo "==> Knative Serving core"
kubectl apply -f "$SERVING/serving-core.yaml"

echo "==> Kourier (networking layer)"
kubectl apply -f "$KOURIER/kourier.yaml"
kubectl patch configmap/config-network -n knative-serving --type merge \
  -p '{"data":{"ingress-class":"kourier.ingress.networking.knative.dev"}}'

# Without a domain Knative only exposes functions inside the cluster
# (*.svc.cluster.local) and Kourier's public gateway does not route to them
kubectl patch configmap/config-domain -n knative-serving --type merge \
  -p '{"data":{"127.0.0.1.sslip.io":""}}'

# Locally built images (dev.local/...) have no registry to resolve tags against
kubectl patch configmap/config-deployment -n knative-serving --type merge \
  -p '{"data":{"registries-skipping-tag-resolving":"dev.local,ko.local"}}'

echo "==> Waiting for Knative pods"
kubectl wait --for=condition=Available deployment --all -n knative-serving --timeout=300s
kubectl wait --for=condition=Available deployment --all -n kourier-system --timeout=300s

echo
kubectl get pods -n knative-serving
kubectl get pods -n kourier-system
