// Poke verifies that Jetstream and Mosquitto are configured properly
package main

import (
	"context"
	"log/slog"
	"net/url"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/eclipse/paho.golang/autopaho"
	"github.com/eclipse/paho.golang/paho"
	"github.com/nats-io/nats.go"
	"github.com/nats-io/nats.go/jetstream"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	connectNATS(ctx)
	connectMosquitto(ctx)
}

func connectNATS(ctx context.Context) {
	stream := "FOO"
	subject1 := "FOO.bar"
	subject2 := "FOO.baz"
	consumerID := "bar"

	nc, err := nats.Connect(nats.DefaultURL)
	if err != nil {
		fatal("failed to connect to NATS", "err", err)
	}

	js, err := jetstream.New(nc)
	if err != nil {
		fatal("failed to create JetStream context", "err", err)
	}

	_, err = js.CreateStream(ctx, jetstream.StreamConfig{
		Name:     stream,
		Subjects: []string{subject1, subject2},
	})
	if err != nil {
		fatal("failed to create stream", "err", err)
	}

	cons, err := js.CreateConsumer(ctx, stream, jetstream.ConsumerConfig{
		Durable:       consumerID,
		FilterSubject: subject1,
		AckPolicy:     jetstream.AckExplicitPolicy,
	})
	if err != nil {
		fatal("failed to create consumer", "err", err)
	}

	received := make(chan []byte, 1)

	_, err = js.PublishAsync(subject1, []byte("Hello World from NATS subject 1"))
	if err != nil {
		fatal("failed to publish to subject", "subject", subject1, "err", err)
	}
	_, err = js.PublishAsync(subject2, []byte("Hello World from NATS subject 2"))
	if err != nil {
		fatal("failed to publish to subject", "subject", subject2, "err", err)
	}

	consContext, err := cons.Consume(func(msg jetstream.Msg) {
		received <- msg.Data()
		if err = msg.Ack(); err != nil {
			fatal("failed to ack message", "err", err)
		}
	})
	if err != nil {
		fatal("failed to start consumer", "err", err)
	}

	defer consContext.Stop()

	select {
	case data := <-received:
		slog.Info("received a JetStream message", "data", data)
	case <-time.After(5 * time.Second):
		fatal("timed out waiting for JetStream message")
	}
}

func connectMosquitto(ctx context.Context) {
	u, err := url.Parse("mqtt://localhost:1883")
	if err != nil {
		fatal("failed to parse broker URL", "err", err)
	}

	clientID := "foo"
	topic := "bar"

	received := make(chan paho.PublishReceived, 1)

	cfg := autopaho.ClientConfig{
		ServerUrls:                    []*url.URL{u},
		KeepAlive:                     20, // Keepalive message should be sent every 20 seconds
		CleanStartOnInitialConnection: false,
		SessionExpiryInterval:         60, // Seconds that a session will survive after disconnection
		OnConnectionUp: func(cm *autopaho.ConnectionManager, _ *paho.Connack) {
			if _, err = cm.Subscribe(context.Background(), &paho.Subscribe{
				Subscriptions: []paho.SubscribeOptions{
					{Topic: topic, QoS: 1},
				},
			}); err != nil {
				fatal("failed to subscribe", "err", err)
			}
		},
		OnConnectError: func(err error) { slog.Error("error whilst attempting connection", "err", err) },
		ClientConfig: paho.ClientConfig{
			ClientID: clientID,
			OnPublishReceived: []func(paho.PublishReceived) (bool, error){
				func(pr paho.PublishReceived) (bool, error) {
					received <- pr
					return true, nil
				}},
			OnClientError: func(err error) { slog.Error("client error", "err", err) },
			OnServerDisconnect: func(d *paho.Disconnect) {
				if d.Properties != nil {
					slog.Warn("server requested disconnect", "reason", d.Properties.ReasonString)
				} else {
					slog.Warn("server requested disconnect; reason code", "reasonCode", d.ReasonCode)
				}
			},
		},
	}

	con, err := autopaho.NewConnection(ctx, cfg)
	if err != nil {
		fatal("failed to create connection", "err", err)
	}

	ctx, cancel := context.WithTimeout(ctx, 5*time.Second)
	defer cancel()

	if err = con.AwaitConnection(ctx); err != nil {
		fatal("failed to await connection", "err", err)
	}

	_, err = con.Publish(ctx, &paho.Publish{
		QoS:     1,
		Topic:   topic,
		Payload: []byte("Hello World from Mosquitto"),
	})
	if err != nil {
		fatal("failed to publish", "err", err)
	}

	select {
	case pr := <-received:
		slog.Info("received Mosquitto message", "topic", pr.Packet.Topic, "payload", pr.Packet.Payload, "retain", pr.Packet.Retain)
	case <-time.After(5 * time.Second):
		fatal("timed out waiting for Mosquitto message")
	}
}

func fatal(err string, params ...any) {
	slog.Error(err, params...)
	os.Exit(1)
}
