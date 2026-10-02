# Seminar notes — DEPOT

Read this before the seminar. It is written in simple words on purpose: the concepts, the numbers you measured, what is not built yet, and likely questions.

## The story in one paragraph

I built a small e-commerce backend as three microservices on Kubernetes, then asked two questions and measured the answers. First: if a server dies while people are shopping, do they notice? Second: if I only run a function when it is needed, how long does the first user wait? The first answer is "no, zero failed requests". The second is "1 to 3 seconds, depending on the language", and that trade-off between cost and latency is the pain point I want to discuss.

## What is running

- **Three services**, each with its own database: users (PostgreSQL), products (MongoDB), carts and orders (PostgreSQL).
- **order-service calls the other two** over HTTP with OpenFeign to check a user exists and to get a product's name and price.
- **Kubernetes on Minikube**, one node, on my laptop. product-service has two copies; the others have one.
- **Two serverless functions on Knative**: `notify-java` and `notify-go`. They do the same thing (receive a notification, log it) in two languages.
- **Checkout triggers the function.** After an order is saved, order-service calls `notify-java` in the background.

## The pain point: scale-to-zero versus cold start

A serverless function that nobody is calling runs **zero** pods, so it costs nothing while idle. When a request arrives, the platform has to start a pod first. That wait is the **cold start**.

### My numbers

Measured 1 October 2026, Apple M1 with 8 GB RAM, single-node Minikube, Knative 1.23. Median of 5 cold starts and 25 warm calls per function; all 70 calls succeeded.

| Function | Cold start | Cold range | Warm | Pre-warmed |
|---|---|---|---|---|
| Java (Spring Boot) | **3248 ms** | 2372–4651 ms | 9 ms | 8 ms |
| Go | **1013 ms** | 917–1157 ms | 6 ms | 6 ms |

Image size: Java 228 MB, Go 7.3 MB.

### What the numbers mean

- **Java's cold start is about 3 times Go's.**
- **About 1 second is the platform, not the language.** Go starts in milliseconds, yet its cold start is still 1 second. That second is Kubernetes scheduling the pod, Knative's sidecar starting, and the held request being forwarded.
- **Java adds about 2.2 seconds** on top: starting the JVM, loading classes, building the Spring context.
- **Once warm they are the same** (9 ms vs 6 ms). The language only matters for the first request.
- **Pre-warming removes the cold start.** Keeping one pod always on (min-scale 1) gave 8 ms and 6 ms. The price is a pod that runs even when nobody calls it.
- Even pre-warmed, Java's very first request took 210 ms against Go's 24 ms, because the JVM still sets things up lazily on first use.

### The trade-off to say out loud

- **Scale to zero**: no cost when idle, but the first user waits 1–3 seconds.
- **Always on**: no wait, but I pay for idle capacity.
- **Sudden spikes** (a flash sale): both react late. Scale-to-zero has to start pods, and an always-on setup has to add more.
- **My idea for future work**: predictive pre-warming. Warm the function shortly before demand is expected, instead of always or never.

### How I measured (if asked)

A script waits until the function has zero pods, sends one request (cold) and five more (warm), and repeats five times. Timing is `curl`'s total time through a port-forward to Knative's gateway. Then it sets min-scale to 1 and measures again. Limits: one laptop, a small sample, and the port-forward adds a little time to every call equally. The numbers are good for comparing, not as absolute cloud figures.

## Self-healing: the proof

I sent a request to product-service every 200 ms for 50 seconds and deleted one of its two pods part-way through.

- **194 requests succeeded, 0 failed.**
- **The replacement pod was ready in about 12 seconds.**

Why nobody noticed, in three parts:

- **Readiness probe.** Kubernetes asks the pod "are you ready?" and sends it traffic only after it says yes. A new pod that is still starting gets no requests.
- **preStop pause.** When a pod is told to stop, it waits 5 seconds first. That gives Kubernetes time to stop routing requests to it.
- **Graceful shutdown.** Requests already in progress are allowed to finish before the process exits.

And the reason a new pod appears at all: I declare "I want 2 copies". Kubernetes constantly compares that **desired state** with the **actual state** and fixes any difference. I never tell it to start a pod.

## Checkout does not wait for the function

When an order is placed, order-service saves it, answers the customer, and only then calls `notify-java` on a separate thread.

- With the function at zero pods, the order came back in **0.97 seconds**.
- The notification arrived about **6 seconds later**, after the function's cold start.
- If the function is down, checkout still succeeds and a warning is logged.

This is how a cold start should be handled: put it where no user is waiting. The weakness is that the notification is one HTTP call with no retry, so it can be lost. A message queue with the outbox pattern fixes that.

## Knative, AWS Lambda, or a normal Deployment

| | Knative (what I use) | AWS Lambda | Deployment + autoscaler |
|---|---|---|---|
| Scales to zero | Yes | Yes | No, at least one pod |
| Cold start | Yes | Yes | No |
| Cost when idle | None for the function, but I run the cluster | None | One pod always |
| Runs where | Any Kubernetes cluster | AWS only | Any Kubernetes cluster |
| Who operates it | Me | AWS | Me |
| Keep one warm | min-scale 1 | Provisioned concurrency | It is always warm |

## My platform and the AWS equivalent

| What I have | AWS equivalent |
|---|---|
| Kubernetes keeping 2 copies running | Auto Scaling group desired capacity |
| Deleting a pod | Terminating an instance |
| Readiness and liveness probes | Load balancer health checks |
| Memory request and limit per pod | Instance size / ECS task memory |
| Service DNS names (`product-service:8082`) | Cloud Map / internal load balancer |
| Knative functions | Lambda |
| min-scale 1 | Provisioned concurrency |
| Kubernetes control loop | The managed control plane |

Not built yet, with their equivalents: a control-plane API and console (EC2/ECS console), an audit log (CloudTrail), admin login and RBAC (IAM).

## Concepts in plain words

- **Database per service.** Each service owns its data and nobody else touches it. Services can change and scale independently; the cost is that I cannot join across them and must call the owner.
- **Price snapshot.** An order line stores the product name and price at purchase time. If the price changes tomorrow, yesterday's order stays correct.
- **Synchronous call.** order-service asks product-service and waits. Simple, but if product-service is down, the cart fails (with a clear 503).
- **Asynchronous call.** After an order, order-service tells the notify function and carries on. The notification can be slow or fail without affecting checkout.
- **Clear errors.** Every error is a small JSON document (ProblemDetail, RFC 7807) with a status and a message: 404 not found, 400 bad request, 503 a service I depend on is down.
- **Requests and limits.** Each pod declares the memory it needs and a hard ceiling, so pods can be packed onto a node and one cannot starve the others.
- **Configuration from the platform.** Service addresses come from environment variables, not from code, so the same image runs locally, in Compose and in Kubernetes.

## Honest limitations

- **One node on one laptop.** If the node dies, everything dies. Self-healing here means pods, not machines.
- **Databases are outside the cluster** in Docker Compose, with no replication.
- **Only product-service has probes and graceful shutdown.** user-service and order-service have one copy each and take about 40 seconds to start.
- **No timeouts or circuit breaker** on the calls between services.
- **The cart depends on product-service for every read.** If product-service is down, the cart cannot be shown.
- **The notification can be lost.** It is one HTTP call with no retry or queue.
- **No security.** No login, no TLS, and the database password is in a properties file.
- **Small measurement sample**, on a machine that was short of memory.
- **The platform costs more memory than the application**: about 900 Mi for Kubernetes and Knative, 775 Mi for my four service pods.

## Roadmap

- **Mini cloud platform (`platform-service`)**: an API to list, scale, terminate and restart services, with an audit log, a login and least-privilege RBAC. The admin app becomes its console.
- **Function console**: invoke, statistics and a pre-warm switch from the API.
- **RabbitMQ and the outbox pattern**: save the order and the event in one database transaction and publish afterwards, so an event is never lost. Kafka later, when several services need to replay the same events.
- **Predictive pre-warming**: warm functions ahead of expected demand.
- **GraalVM native image** for `notify-java`, to see how close Java can get to Go's cold start.
- **Resilience**: timeouts, circuit breaker, retries for reads only.
- **Small extras**: a header showing which pod served a request, Java 21 virtual threads, an autoscaling policy with a load test.
- **Saga for checkout**: undo steps cleanly when one service fails half-way.
- **Security**: real authentication, per-user roles, TLS, secrets management, network policies.
- **A single gateway**, and the databases inside the cluster.

## Likely questions

1. **Why is Java slower to start than Go?**
   Go compiles to one small native binary. Java has to start a virtual machine, load thousands of classes and build the Spring context before it can answer.

2. **Could you make Java start faster?**
   Yes: compile it ahead of time with GraalVM native image, or use a lighter framework. AWS has SnapStart for the same problem. I have not measured these yet; it is on my roadmap.

3. **Why does Go still take a second?**
   That is the platform: scheduling the pod, starting the Knative sidecar and forwarding the held request. It is the floor for any language on my setup.

4. **Are your numbers reliable?**
   They are from one laptop with five cold starts per function, so they show the pattern, not exact values. The ranges did not overlap: Java 2.4–4.7 s, Go 0.9–1.2 s.

5. **Why Knative and not AWS Lambda?**
   I wanted to see and measure what happens inside. Knative runs on any Kubernetes cluster and the behaviour (scale to zero, cold starts) is the same idea.

6. **What happens if the node itself fails?**
   Everything stops; I have one node. A real cluster has several nodes and Kubernetes moves the pods to a healthy one.

7. **Why a database per service?**
   So each service can change and scale on its own. The cost is no joins across services and more network calls.

8. **What if product-service is down during checkout?**
   Checkout fails fast with a clear 503 "Product service unavailable". What is missing is a timeout and a circuit breaker, so a slow product-service cannot hang checkout.

9. **Why were there zero failed requests when you killed a pod?**
   There were two copies; the readiness probe kept traffic away from the starting pod; the dying pod waited 5 seconds and finished its requests before exiting.

10. **Is this secure?**
    No. It is a demo with no authentication. Security is listed as future work.

11. **How much does all this cost in resources?**
    About 2.1 GB of memory for the whole cluster when idle, and nearly half of that is the platform itself.
