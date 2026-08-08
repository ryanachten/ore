.PHONY: build
build:
	./gradlew build --configuration-cache
up:
	docker compose up -d
down:
	docker compose down
logs:
	docker compose logs
