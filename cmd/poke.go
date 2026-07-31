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
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	connectNATS()
	connectMosquitto(ctx)
}

func connectNATS() {
	subject := "foo"

	nc, err := nats.Connect(nats.DefaultURL)
	if err != nil {
		panic(err)
	}

	_, err = nc.Subscribe(subject, func(m *nats.Msg) {
		slog.Info("received NATS message", "data", string(m.Data))
	})
	if err != nil {
		panic(err)
	}

	if err = nc.Flush(); err != nil {
		panic(err)
	}

	if err = nc.Publish(subject, []byte("Hello World from NATS")); err != nil {
		panic(err)
	}

	if err = nc.Flush(); err != nil {
		panic(err)
	}

	time.Sleep(500 * time.Millisecond)
}

func connectMosquitto(ctx context.Context) {
	clientID := "foo"
	topic := "bar"

	u, err := url.Parse("mqtt://localhost:1883")
	if err != nil {
		panic(err)
	}

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
				slog.Error("failed to subscribe", "err", err)
			}
		},
		OnConnectError: func(err error) { slog.Error("error whilst attempting connection", "err", err) },
		ClientConfig: paho.ClientConfig{
			ClientID: clientID,
			OnPublishReceived: []func(paho.PublishReceived) (bool, error){
				func(pr paho.PublishReceived) (bool, error) {
					slog.Info("received Mosquitto message", "topic", pr.Packet.Topic, "payload", pr.Packet.Payload, "retain", pr.Packet.Retain)
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
		panic(err)
	}

	if err = con.AwaitConnection(ctx); err != nil {
		panic(err)
	}

	_, err = con.Subscribe(ctx, &paho.Subscribe{
		Subscriptions: []paho.SubscribeOptions{
			{Topic: topic, QoS: 1},
		},
	})
	if err != nil {
		panic(err)
	}

	_, err = con.Publish(ctx, &paho.Publish{
		QoS:     1,
		Topic:   topic,
		Payload: []byte("Hello World from Mosquitto"),
	})
	if err != nil {
		panic(err)
	}

	time.Sleep(500 * time.Millisecond)
}
