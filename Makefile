.PHONY: build test test-backend test-frontend lint lint-backend lint-frontend lint-fix lint-fix-backend lint-fix-frontend up down logs run-frontend
build: lint-fix
	./gradlew build --configuration-cache
test: test-backend test-frontend
test-backend:
	./gradlew test --configuration-cache
test-frontend:
	cd frontend && bun run test
lint: lint-backend lint-frontend
lint-backend:
	./gradlew spotlessCheck checkstyleMain checkstyleTest
lint-frontend:
	cd frontend && bun run lint
	cd frontend && bun run typecheck
lint-fix: lint-fix-backend lint-fix-frontend
lint-fix-backend:
	./gradlew spotlessApply
lint-fix-frontend:
	cd frontend && bun run lint:fix
up:
	docker compose up -d
up-build: build
	docker compose up -d --build
down:
	docker compose down
logs:
	docker compose logs
run-frontend:
	cd frontend && bun run dev --open