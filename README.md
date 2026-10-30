# realscore

Spring Boot service for scoring real estate properties.

## Requirements

- Java 25
- Maven
- PostgreSQL (for running against a real database; tests use an in-memory H2 database)

## Running locally

Start a local PostgreSQL instance:

```
docker compose up -d
```

This matches the datasource defaults (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` default to `localhost`, `5432`, `realscore`, `realscore`, `realscore`) — override via env vars to point at a different database.

```
mvn spring-boot:run
```

## Testing

```
mvn test
```

## API

- `GET /api/properties` — list properties
- `POST /api/properties` — create a property
- `GET /api/properties/{id}` — fetch a property
- `GET /api/properties/{id}/score` — compute a property's score
