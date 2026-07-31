up:
	docker compose up -d
down:
	docker compose down
logs:
	docker compose logs
run:
	go run main.go
lint:
	golangci-lint run
test:
	go test ./...