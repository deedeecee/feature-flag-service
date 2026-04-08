.PHONY: dev stop restart build run test migrate clean logs ps

# ── Infrastructure ────────────────────────────────────────────────
dev:
	docker compose up -d
	@echo "Postgres on :5432, Redis on :6379"

stop:
	docker compose down

restart: stop dev

logs:
	docker compose logs -f

ps:
	docker compose ps

# ── Application ───────────────────────────────────────────────────
build:
	./mvnw package -DskipTests

run:
	./mvnw spring-boot:run -Dspring-boot.run.profiles=local

test:
	./mvnw test

# ── Database ──────────────────────────────────────────────────────
migrate:
	./mvnw flyway:migrate

migrate-info:
	./mvnw flyway:info

migrate-clean:
	./mvnw flyway:clean

# ── Full stack (containerised) ────────────────────────────────────
up:
	docker compose --profile full up --build -d

down:
	docker compose --profile full down

# ── Housekeeping ──────────────────────────────────────────────────
clean:
	./mvnw clean
	docker compose down -v