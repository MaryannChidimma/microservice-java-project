#!/usr/bin/env bash
# Measure cold vs warm latency of the two Knative functions (Java vs Go),
# then the same with one pod kept warm (min-scale 1).
#
#   ./scripts/coldstart.sh                 # 5 cold runs per function
#   COLD_RUNS=10 ./scripts/coldstart.sh
#
# Calls go through Kourier on a local port-forward (8091) with the function's
# Host header. Every measurement is saved to docs/results/coldstart-<date>.csv.
set -euo pipefail
cd "$(dirname "$0")/.."

FUNCTIONS=(notify-java notify-go)
COLD_RUNS=${COLD_RUNS:-5}
WARM_CALLS=${WARM_CALLS:-5}
GATEWAY_PORT=8091
OUT="docs/results/coldstart-$(date +%F).csv"

mkdir -p docs/results
echo "function,mode,run,call,latency_s,http_code" > "$OUT"

kubectl port-forward -n kourier-system svc/kourier "$GATEWAY_PORT:80" >/dev/null 2>&1 &
PF_PID=$!
trap 'kill $PF_PID 2>/dev/null || true' EXIT
until nc -z 127.0.0.1 "$GATEWAY_PORT" 2>/dev/null; do sleep 1; done

host_of() { kubectl get ksvc "$1" -o jsonpath='{.status.url}' | sed 's#^https\{0,1\}://##'; }
pods_of() { kubectl get pods -l "serving.knative.dev/service=$1" --no-headers 2>/dev/null | wc -l | tr -d ' '; }

wait_for_zero() {
  printf "   waiting for %s to scale to zero" "$1"
  until [ "$(pods_of "$1")" = "0" ]; do printf "."; sleep 3; done
  echo
}

# call <function> <mode> <run> <call-number>  -> appends one CSV line, prints the latency
call() {
  local result
  result=$(curl -s -o /dev/null -m 120 -w '%{time_total},%{http_code}' -X POST \
    -H "Host: $(host_of "$1")" -H 'Content-Type: application/json' \
    -d '{"orderId":1,"event":"OrderPlaced"}' "http://127.0.0.1:$GATEWAY_PORT/notify") || result="0,000"
  echo "$1,$2,$3,$4,$result" >> "$OUT"
  echo "   $2 run $3 call $4: ${result%,*} s (HTTP ${result#*,})"
}

set_min_scale() {
  kubectl patch ksvc "$1" --type merge \
    -p "{\"spec\":{\"template\":{\"metadata\":{\"annotations\":{\"autoscaling.knative.dev/min-scale\":\"$2\"}}}}}" >/dev/null
  # Give the controller a moment to start the new revision before waiting on it
  sleep 3
  kubectl wait --for=condition=Ready "ksvc/$1" --timeout=180s >/dev/null
}

for f in "${FUNCTIONS[@]}"; do
  echo "==> $f: cold and warm (scale to zero)"
  for run in $(seq 1 "$COLD_RUNS"); do
    wait_for_zero "$f"
    call "$f" cold "$run" 1
    for c in $(seq 2 $((WARM_CALLS + 1))); do
      call "$f" warm "$run" "$c"
    done
  done

  echo "==> $f: pre-warmed (min-scale 1)"
  set_min_scale "$f" 1
  # Old revision's pods must be gone so only the kept-warm pod answers
  until [ "$(pods_of "$f")" = "1" ]; do sleep 3; done
  for c in $(seq 1 "$WARM_CALLS"); do
    call "$f" prewarmed 1 "$c"
  done
  set_min_scale "$f" 0
done

median_ms() { # <function> <mode>
  awk -F, -v f="$1" -v m="$2" '$1==f && $2==m && $6==200 {print $5}' "$OUT" | sort -n |
    awk '{a[NR]=$1} END {if (NR==0) {print "n/a"; exit}
         v = (NR%2) ? a[(NR+1)/2] : (a[NR/2]+a[NR/2+1])/2; printf "%.0f ms", v*1000}'
}

echo
printf "%-14s %-14s %-14s %-14s\n" "function" "cold (median)" "warm (median)" "pre-warmed"
for f in "${FUNCTIONS[@]}"; do
  printf "%-14s %-14s %-14s %-14s\n" "$f" "$(median_ms "$f" cold)" "$(median_ms "$f" warm)" "$(median_ms "$f" prewarmed)"
done
echo
echo "Saved: $OUT"
