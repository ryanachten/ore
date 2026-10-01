GRADLE_BUILD = ./gradlew build --configuration-cache

.PHONY: build build-backend build-frontend install-frontend verify test test-backend test-frontend lint lint-backend lint-frontend lint-fix lint-fix-backend lint-fix-frontend up down logs run-frontend
build: lint-fix
	$(GRADLE_BUILD)
build-backend:
	$(GRADLE_BUILD)
build-frontend:
	cd frontend && bun run build
install-frontend:
	cd frontend && bun install --frozen-lockfile
verify: lint test build-frontend
	$(GRADLE_BUILD)
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