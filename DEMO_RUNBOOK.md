# Demo runbook — seminar, 2 October 2026, 09:00

Run these in order. Each step says what you should see. Steps marked **untested** were not run during the preparation session. Everything else, including stopping and restarting the cluster, was tested on 1 October.

## Before you leave home

- Close every application you do not need. The Mac has 8 GB and was swapping heavily last night.
- Laptop on power. You need internet for the public link, not for the cluster itself.

## 1. Start (about 5 minutes)

```bash
# 1. Docker Desktop must be running first
docker info >/dev/null && echo "Docker OK"

# 2. Databases
cd ~/Downloads/demo
docker compose up -d user-db order-db product-db

# 3. Cluster (keeps last night's deployments, Knative and images)
minikube start

# 4. Wait for the pods
kubectl get pods -w
```

You should see six pods reach `1/1 Running`: two `product-service`, and one each of `user-service`, `order-service`, `api-gateway` and `storefront`. user-service and order-service take about 40 seconds to start. Press Ctrl+C when they are ready.

- A restart count of 1 or 2 is normal here: after a cluster restart the services can start before the database connection is reachable, exit, and start again on their own.
- If pods do not come back, run `./scripts/deploy.sh`.
- Do **not** run `minikube delete` unless section 6 tells you to.

## 2. Health checklist

```bash
kubectl get pods                       # 6 pods, 1/1 Running (4 services, api-gateway, storefront)
kubectl get pods -n knative-serving    # 5 pods Running
kubectl get pods -n kourier-system     # 1 pod Running
kubectl get ksvc                       # notify-java and notify-go, READY True
kubectl top nodes                      # memory under about 70%
```

The three entry points:

| Entry point | Address | Who uses it |
|---|---|---|
| Backend (api-gateway) | `http://localhost:8000` | you, for checks and `curl` |
| Frontend (storefront) | `http://localhost:8080` | you, and the audience through the tunnel |
| Admin | `http://localhost:5174` | only you, on the projector |

Backend check (terminal A, leave running):

```bash
kubectl port-forward svc/api-gateway 8000:80
```

```bash
curl -s -o /dev/null -w '%{http_code}\n' localhost:8000/products       # 200
curl -s -o /dev/null -w '%{http_code}\n' localhost:8000/users          # 200
curl -s -o /dev/null -w '%{http_code}\n' localhost:8000/carts/1        # 200, proves order-service reaches the other two
```

Function check (terminal B, leave running):

```bash
kubectl port-forward -n kourier-system svc/kourier 8091:80
```

```bash
curl -s -o /dev/null -w '%{http_code} in %{time_total}s\n' -X POST \
  -H "Host: notify-go.default.127.0.0.1.sslip.io" -d 'test' http://127.0.0.1:8091/notify
```

Expect `200` in about 1 second the first time and a few milliseconds the second time.

## 3. Storefront and public link

The storefront is already deployed in the cluster and comes back with `minikube start`. Check it and open the link:

```bash
kubectl get pods -l app=storefront              # 1/1 Running
kubectl port-forward svc/storefront 8080:80     # terminal C, leave running
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/api/products   # 200
cloudflared tunnel --url http://localhost:8080  # terminal D: prints the public URL
```

- The storefront through `localhost:8080` was tested. The `cloudflared` tunnel is **untested**: it was not installed last night (`brew install cloudflared`).
- Fallback tunnel: `ngrok http 8080`.
- To redeploy it: in `~/microservices-FE`, run `docker build -t storefront:1.0 . && minikube image load storefront:1.0 && kubectl apply -f k8s/storefront.yaml && kubectl rollout restart deployment/storefront`.
- If the storefront is not ready, demo with the terminal commands in section 5. They need no frontend.

## 4. Admin console (projector, localhost only)

```bash
kubectl proxy --port=8001                                   # terminal E, leave running
kubectl port-forward -n kourier-system svc/kourier 8091:80  # terminal B from section 2
cd ~/microserce-admin-ecommerce/admin
npm run build && npm run preview                            # http://localhost:5174/admin
```

- Switch the environment to **Minikube** in the top bar.
- **Serverless** page: Invoke each function at zero pods (cold), then again (warm); toggle Pre-warm.
- **Pod Recovery** page: the counters at the top send `GET /products` every 500 ms. Delete a product-service pod and watch "Failed" stay at 0.
- The routes behind both pages were tested with `curl`. The pages themselves were **not opened in a browser** during preparation, so click through them once before the talk.
- If a page says the Kubernetes API is not reachable, stop `kubectl proxy` and start it again: it keeps pointing at the cluster it was started against.
- Never put the admin behind the public tunnel.

## 5. The demo

### Part 1: self-healing

Terminal 1, watch the pods:

```bash
kubectl get pods -l app=product-service -w
```

Terminal 2, delete one:

```bash
kubectl get pods -l app=product-service
kubectl delete pod <one-of-the-two-names>
```

What the audience sees: the pod goes `Terminating`, a new one appears at once and is `1/1 Running` in about 12 seconds, and the shop keeps answering. Last night: 194 requests during a delete, 0 failed.

You can also do this from the admin's Pod Recovery page (section 4).

Only delete **product-service** pods. user-service and order-service have one replica and no probes, so deleting them causes about 40 seconds of errors.

### Part 2: serverless cold start

Keep terminal B's Kourier port-forward running. Watch the function pods:

```bash
kubectl get pods -l app.kubernetes.io/part-of=depot-serverless -w
```

Show there are none ("scaled to zero"), then call Java and Go:

```bash
curl -s -o /dev/null -w 'java: %{time_total}s\n' -X POST -H "Host: notify-java.default.127.0.0.1.sslip.io" -d 'order placed' http://127.0.0.1:8091/notify
curl -s -o /dev/null -w 'go:   %{time_total}s\n' -X POST -H "Host: notify-go.default.127.0.0.1.sslip.io" -d 'order placed' http://127.0.0.1:8091/notify
```

Run both again straight away to show the warm time. Expect about 3 s then 10 ms for Java, about 1 s then 6 ms for Go. Wait under a minute and the pods disappear again.

### Part 3: checkout does not wait for the function

With `notify-java` at zero pods and the gateway port-forward from section 2 running, add an item and place an order (user 1 is a test user):

```bash
curl -s -X POST localhost:8000/carts/1/items -H 'Content-Type: application/json' -d '{"productId":"6abb86fe405dce20a7bea0c6","quantity":1}'
curl -s -w '\n%{time_total}s\n' -X POST localhost:8000/orders/1
kubectl logs deploy/order-service --tail=20 | grep Notification
```

The order returns in about a second. A few seconds later a `notify-java` pod appears and the log shows `Notification sent for order N`.

### Part 4: pre-warming

```bash
kubectl patch ksvc notify-java --type merge -p '{"spec":{"template":{"metadata":{"annotations":{"autoscaling.knative.dev/min-scale":"1"}}}}}'
```

One pod now stays up and the first call is fast. Set it back afterwards with the same command and `"0"`.

### If you would rather show the full measurement

```bash
./scripts/coldstart.sh
```

It takes about 10 minutes, so run it before the talk or show last night's file: `docs/results/coldstart-2026-10-01.csv`.

## 6. If something fails

| Symptom | Do this |
|---|---|
| Pods missing or not `Running` after `minikube start` | `./scripts/deploy.sh` |
| user-service or order-service in `CrashLoopBackOff` | The databases were not up. `docker compose up -d user-db order-db product-db`, then `kubectl rollout restart deployment user-service order-service` |
| `deploy.sh` stops on a Maven download error | Run it again; it was a transient network error last night |
| A pod shows `OOMKilled` (`kubectl describe pod <name>`) | Raise its memory limit to `448Mi` in `k8s/`, then `kubectl apply -f k8s/` |
| Node memory above about 85% | `kubectl scale deployment product-service --replicas=1` (then do not demo pod deletion) |
| An API call returns a JSON error with `status` and `detail` | That is the new error format working: 404 not found, 400 bad request, 503 a dependency is down |
| Function call returns 404 | The `Host` header is wrong. Check `kubectl get ksvc` for the exact host |
| Function call hangs | `kubectl get pods -n knative-serving`; if one is not Running, `./scripts/install-knative.sh` |
| `kubectl top` says metrics not available | Wait a minute, or `minikube addons enable metrics-server` |
| Port-forward dies | Start it again; they drop when a pod restarts |
| The shop loads but shows "restarting" for everything | The gateway is down or was changed: `kubectl rollout restart deployment/api-gateway` |
| Cluster is beyond repair (about 10 minutes, needs internet) | `minikube delete && minikube start --cpus=4 --memory=3600 && ./scripts/install-knative.sh && ./scripts/deploy.sh` |
| No internet in the room | Skip the tunnel; present from the laptop on the projector |
| Nothing works live | Show `docs/results/coldstart-2026-10-01.csv` and the tables in `docs/SEMINAR_NOTES.md`, and present the rest as recorded results |

## 7. After the seminar

```bash
minikube stop
docker compose stop
```
