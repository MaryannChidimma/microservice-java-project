package main

import (
	"io"
	"log"
	"net/http"
	"os"
)

func notify(w http.ResponseWriter, r *http.Request) {
	body, _ := io.ReadAll(r.Body)
	log.Printf("notification received: %s", body)
	w.WriteHeader(http.StatusOK)
}

func main() {
	// Knative tells the container which port to listen on through PORT
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	http.HandleFunc("POST /notify", notify)
	log.Printf("notify-go listening on :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, nil))
}
