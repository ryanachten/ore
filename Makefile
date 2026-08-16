.PHONY: build test lint lint-fix up down logs
build:
	./gradlew build --configuration-cache
test:
	./gradlew test --configuration-cache
lint:
	./gradlew spotlessCheck checkstyleMain checkstyleTest spotbugsMain spotbugsTest
lint-fix:
	./gradlew spotlessApply
up:
	docker compose up -d
up-build:
	docker compose up -d --build
down:
	docker compose down
logs:
	docker compose logs