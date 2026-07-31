run:
	go run main.go
lint:
	golangci-lint run
test:
	go test ./...