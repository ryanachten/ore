.PHONY: build test lint lint-fix up down logs
build: lint-fix
	./gradlew build --configuration-cache
test:
	./gradlew test --configuration-cache
lint:
	./gradlew spotlessCheck checkstyleMain checkstyleTest
lint-fix:
	./gradlew spotlessApply
up:
	docker compose up -d
up-build: build
	docker compose up -d --build
down:
	docker compose down
logs:
	docker compose logs