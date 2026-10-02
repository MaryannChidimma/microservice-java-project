FROM golang:1.25-alpine AS build
WORKDIR /app
COPY go.mod main.go ./
RUN CGO_ENABLED=0 go build -ldflags="-s -w" -o notify .

FROM gcr.io/distroless/static-debian12:nonroot
COPY --from=build /app/notify /notify
EXPOSE 8080
ENTRYPOINT ["/notify"]
