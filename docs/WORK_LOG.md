# Work log — 1 October 2026

What was done in the evening session before the seminar, what to add next, and how much memory each part uses.

## What was done

| # | Step | Result |
|---|---|---|
| 1 | Deploy script and low-memory cluster | `scripts/deploy.sh` rebuilds and deploys everything. Cluster recreated with 4 CPUs and 3600 MB. product-service runs 2 replicas, user-service and order-service 1 each, 384Mi limit per Java pod. |
| 2 | Feign URL fix | order-service now reaches product-service inside the cluster. Adding an item to a cart returns 200; before, it returned 500. |
| 3 | Knative Serving + Kourier 1.23.0 | Installed by `scripts/install-knative.sh`. Six pods, all healthy on Kubernetes 1.37. |
| 4 | Two functions | `notify-java` (Spring Boot) and `notify-go` (Go standard library), identical behaviour, both scale to zero. |
| 5 | Cold-start measurements | `scripts/coldstart.sh`; 70 calls, all HTTP 200. Java 3248 ms cold, Go 1013 ms cold, both under 10 ms warm. |
| 6 | product-service survives a pod termination | Actuator, probes, preStop and graceful shutdown. 194 requests during a pod delete, 0 failed; replacement Ready in about 12 s. |
| 7 | Documents | README, seminar notes, runbook, this log. |
| 8 | Error handling in all three services | ProblemDetail JSON: 404 for unknown user, product or cart; 400 for a bad cart body or an empty cart; 503 when product-service is down. |
| 9 | Async notification | order-service calls `notify-java` after the order commits. An order returned in 0.97 s while the function was at zero pods; the notification arrived about 6 s later. |
| 11 | Standalone API gateway | `k8s/api-gateway.yaml`: one nginx pod (8 Mi) routing `/users`, `/products`, `/carts`, `/orders`. The storefront now forwards `/api` to it. Backend on `localhost:8000`, storefront on `8080`, admin on `5174`. 120 requests through shop and gateway during a pod delete: 0 failed. |
| 10 | Combined test, including a cluster restart | `minikube stop` and `start`: all pods, Knative and both functions back in about a minute. Second pod-termination test: 202 requests, 0 failed. |

## Problems found and fixed

- **Feign property mismatch.** `ProductClient` read `product-service.url` but Kubernetes set `CLIENTS_PRODUCT_SERVICE_URL`. In the cluster order-service called `localhost:8082` and every cart call that needed a product failed with `Connection refused`. Both clients now use `clients.<name>.url`, and Compose and Kubernetes use the same env var names.
- **Functions not reachable from outside.** With no domain configured, Knative gave the functions only a cluster-local address and Kourier's public gateway did not route to them. The install script now sets the domain to `127.0.0.1.sslip.io`.
- **springdoc 2.6.0 broke when the error handler was added.** It targets Spring Boot 3, and on Boot 4 it fails with `NoSuchMethodError` as soon as a `@RestControllerAdvice` exists, so `/v3/api-docs` returned 500. `springdoc.override-with-generic-response=false` stops it scanning the handler; the docs and Swagger UI work again. The proper fix is upgrading springdoc to a Boot 4 release.
- **A transient Maven download error** (`bad_record_mac` from Maven Central) failed one product-service build. Re-running the build fixed it. If `deploy.sh` stops on a download error, run it again.

## Where the session differs from the original plan

- **3600 MB, not 4000.** Docker Desktop was still set to about 3.9 GB, so 4000 MB was not possible. Everything fit anyway (see below).
- **Container runtime is containerd.** The recreated cluster uses containerd, so images are copied in with `minikube image load`; `minikube docker-env` does not apply.
- **`default-domain` skipped.** It waits for an external IP that Minikube only gives with `minikube tunnel`. A fixed domain plus a port-forward on 8091 replaces it.
- **Probes on product-service only.** user-service and order-service have no Actuator or probes yet.
- **`server.error.include-stacktrace` not set.** The error handler now controls every error body, and none of them contain a stack trace.

## Memory footprint

Measured with `kubectl top` and `docker stats` at 23:10, system idle, functions scaled to zero.

| Component | Memory | Notes |
|---|---|---|
| Kubernetes system pods | 709 Mi | API server alone is 372 Mi |
| Knative Serving + Kourier (6 pods) | 184 Mi | The node grew by about 330 Mi when they were installed |
| order-service | 218 Mi | 384Mi limit |
| user-service | 209 Mi | 384Mi limit |
| product-service × 2 | 174 Mi each | 384Mi limit |
| `notify-java`, `notify-go` | 0 while idle | Not measured awake. Limits: 384Mi (Java), 64Mi (Go) |
| Kubelet, container runtime, OS | about 460 Mi | Node total minus the pods above |
| Storefront (nginx, 1 pod) | 7 Mi | Added later; 64Mi limit |
| API gateway (nginx, 1 pod) | 8 Mi | Added later; 64Mi limit |
| **Node total** | **2127 Mi (54%)** | Of a 3600 MB node, before the storefront |
| Three databases (Compose) | 96 Mi | Outside the cluster |

What the numbers say:

- **The platform costs more than the application.** Kubernetes and Knative together use about 900 Mi before any of your code runs; the four service pods use 775 Mi.
- **A Spring Boot pod idles at roughly 175–220 Mi.** Each extra replica costs about that much, which is why only product-service has two.
- **There is about 1.4 GB of headroom on the node**, enough for the storefront and both functions awake.
- **The Mac is the real limit.** It has 8 GB and was using about 12 GB of swap during the session. Close other applications before the demo.
- Before the change, the old cluster (six Java pods, 3 GB limit) sat at 97% of its memory.

How to check at any time:

```bash
kubectl top nodes
kubectl top pods -A
docker stats --no-stream
```

Above about 85% on the node, scale product-service to 1 (`kubectl scale deployment product-service --replicas=1`) before doing anything heavy.

## What to add next

In the order I would do them:

1. **A real Ingress.** The backend now has its own nginx gateway (`api-gateway`), reached by port-forward. An Ingress controller would be the standard Kubernetes way to expose it, and the place to add authentication and rate limiting.
2. **Probes and graceful shutdown for user-service and order-service.** They take about 40 s to start and receive traffic before they are ready.
3. **Timeouts and a circuit breaker on the Feign clients**, so a slow product-service cannot hang checkout.
4. **Credentials out of the code.** The Postgres password is in `application.properties`; move it to a Kubernetes Secret.
5. **Databases in the cluster** as StatefulSets with persistent volumes, so the system does not depend on the laptop's Compose.
6. **platform-service**: the mini control-plane API (inventory, scale, terminate, restart, audit log) with a login and least-privilege RBAC.
7. **Messaging**: RabbitMQ with the outbox pattern. Today the notification is a single HTTP call with no retry, so it is lost if the function is unreachable.
8. **Cold-start work**: a GraalVM native image of `notify-java`, and predictive pre-warming.
9. **Stock handling.** `stockQuantity` exists but placing an order never checks or reduces it.
10. **Tests.** Each service has only the generated context-load test.
11. **Upgrade springdoc** to a Spring Boot 4 release and remove the workaround property.

## Files changed

New:
- `scripts/deploy.sh`, `scripts/install-knative.sh`, `scripts/coldstart.sh`
- `notify-java/`, `notify-go/`
- `k8s/knative/notify-java.yaml`, `k8s/knative/notify-go.yaml`
- `docs/results/coldstart-2026-10-01.csv`
- `README.md`, `DEMO_RUNBOOK.md`, `docs/SEMINAR_NOTES.md`, `docs/WORK_LOG.md`

Edited:
- `k8s/user-service-deployment.yaml`, `k8s/order-service-deployment.yaml`: 1 replica, memory settings.
- `k8s/product-service-deployment.yaml`: memory settings, probes, preStop, grace period, `depot.platform/managed` label.
- `order-service/.../client/ProductClient.java`: property name.
- `docker-compose.yml`: env var name for order-service.
- `product-service/pom.xml`, `product-service/src/main/resources/application.properties`: Actuator and graceful shutdown.
- `*/exception/` (new in each service): the exceptions and `GlobalExceptionHandler`.
- `order-service/.../service/CartService.java`, `OrderService.java`, `controller/CartController.java`, `user-service/.../service/AddressService.java`: typed exceptions and cart body validation.
- `order-service/.../notification/` (new), `OrderServiceApplication.java`, `k8s/order-service-deployment.yaml`: the async notification and its `NOTIFICATIONS_URL`.
- The three `application.properties`: the springdoc workaround.
