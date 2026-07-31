// Poke verifies that Jetstream and Mosquitto are configured properly
package main

import (
	"fmt"
	"log/slog"
	"os"
	"time"

	"github.com/nats-io/nats.go"
)

func main() {
	connectNATS()
}

func connectNATS() {
	jetstreamSubject := "foo"

	nc, err := nats.Connect(nats.DefaultURL)
	if err != nil {
		slog.Error("error connecting to NATS server", "err", err)
		os.Exit(1)
	}

	_, err = nc.Subscribe(jetstreamSubject, func(m *nats.Msg) {
		fmt.Printf("Received a message: %s\n", string(m.Data))
	})
	if err != nil {
		slog.Error("error subscribing to subject on Jetstream", "err", err)
		os.Exit(1)
	}

	if err = nc.Flush(); err != nil {
		slog.Error("error flushing server", "err", err)
		os.Exit(1)
	}

	if err = nc.Publish(jetstreamSubject, []byte("Hello World")); err != nil {
		slog.Error("error publishing message to Jetstream", "err", err)
		os.Exit(1)
	}

	if err = nc.Flush(); err != nil {
		slog.Error("error flushing server", "err", err)
		os.Exit(1)
	}

	time.Sleep(500 * time.Millisecond)
}
