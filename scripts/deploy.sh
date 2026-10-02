#!/usr/bin/env bash
# Build the three services and deploy them to Minikube.
#
# First time (or after changing memory/CPU, which needs a fresh cluster):
#   minikube delete
#   minikube start --cpus=4 --memory=4000
#
# Then, from anywhere:
#   ./scripts/deploy.sh
#
# Image tags are reused (:1.0), so the script ends with a rollout restart
# to make the pods pick up the new images.
set -euo pipefail
cd "$(dirname "$0")/.."

SERVICES=(user-service product-service order-service)
TAG=1.0
FUNCTIONS=(notify-java notify-go)
FUNCTION_TAG=v1

if ! minikube status >/dev/null 2>&1; then
  echo "Minikube is not running. Start it first:"
  echo "  minikube start --cpus=4 --memory=4000"
  exit 1
fi

echo "==> Databases up; Compose copies of the services stopped (pods replace them)"
docker compose up -d user-db order-db product-db
docker compose stop "${SERVICES[@]}"

echo "==> metrics-server (needed for 'kubectl top')"
minikube addons enable metrics-server

# Built with the laptop's Docker to reuse its layer cache, then copied into Minikube
for s in "${SERVICES[@]}"; do
  echo "==> Building $s:$TAG"
  docker build -t "$s:$TAG" "./$s"
done

for s in "${SERVICES[@]}"; do
  echo "==> Loading $s:$TAG into Minikube"
  minikube image load "$s:$TAG"
done

echo "==> Applying manifests"
kubectl apply -f k8s/

echo "==> Restarting so pods use the new images"
for s in "${SERVICES[@]}"; do
  kubectl rollout restart "deployment/$s"
done
for s in "${SERVICES[@]}"; do
  kubectl rollout status "deployment/$s" --timeout=240s
done

# The gateway reads its nginx config from a ConfigMap only when it starts
kubectl rollout restart deployment/api-gateway
kubectl rollout status deployment/api-gateway --timeout=120s

# Serverless functions, only once Knative is installed (scripts/install-knative.sh)
if kubectl get crd services.serving.knative.dev >/dev/null 2>&1; then
  for f in "${FUNCTIONS[@]}"; do
    echo "==> Building and loading dev.local/$f:$FUNCTION_TAG"
    docker build -t "dev.local/$f:$FUNCTION_TAG" "./$f"
    minikube image load "dev.local/$f:$FUNCTION_TAG"
  done
  kubectl apply -f k8s/knative/
  kubectl wait --for=condition=Ready ksvc --all --timeout=180s
  kubectl get ksvc
else
  echo "==> Knative not installed; skipping functions (run scripts/install-knative.sh)"
fi

echo
kubectl get pods -o wide
