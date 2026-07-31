up:
	docker compose up -d
down:
	docker compose down
docker:
	docker compose down
run:
	go run main.go
lint:
	golangci-lint run
test:
	go test ./...