.PHONY: build
build:
	./gradlew build --configuration-cache
lint:
	./gradlew spotlessCheck checkstyleMain checkstyleTest spotbugsMain spotbugsTest
lint-fix:
	./gradlew spotlessApply
up:
	docker compose up -d
down:
	docker compose down
logs:
	docker compose logs