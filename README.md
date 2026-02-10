# MS Customer Service

Customer management microservice for Challenge Bank. Built with Quarkus 3.15 LTS.

## Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Quarkus | 3.15.3 LTS | Main framework |
| Java | 21 | Language |
| PostgreSQL | 16 | Database |
| Hibernate ORM + Panache | - | ORM (Repository pattern) |
| SmallRye JWT | - | JWT authentication |
| Micrometer + Prometheus | - | Metrics |
| SmallRye Health | - | Health checks |
| Flyway | - | DB migrations |
| Docker | - | Containerization |

## Architecture

```
controller/          -> Presentation layer (REST endpoints)
service/             -> Business logic
repository/          -> Data access (Panache Repository pattern)
model/entity/        -> JPA entities
model/dto/request/   -> Input DTOs
model/dto/response/  -> Output DTOs
model/enums/         -> Enums
mapper/              -> Entity <-> DTO mapping
exception/           -> Exceptions and global handler
config/              -> Configuration (metrics)
health/              -> Custom health checks
logging/             -> Logging filters (Correlation ID)
```

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker and Docker Compose (for container-based execution)

## Running the application

### Development mode (with local PostgreSQL)

First, start PostgreSQL:

```bash
docker-compose up -d postgres
```

Then, run Quarkus in dev mode:

```bash
./mvnw quarkus:dev
```

### With Docker Compose (all-in-one)

```bash
docker-compose up --build
```

The service will be available at `http://localhost:8080`.

## Endpoints

### Customers (CRUD)

| Method | Path | Description | Role |
|---|---|---|---|
| GET | `/v1/customers` | List customers (paginated) | USER, ADMIN |
| POST | `/v1/customers` | Create customer | ADMIN |
| GET | `/v1/customers/{customerId}` | Get by ID | USER, ADMIN |
| PUT | `/v1/customers/{customerId}` | Update customer | ADMIN |
| DELETE | `/v1/customers/{customerId}` | Delete customer (soft delete) | ADMIN |
| GET | `/v1/customers/document/{documentId}` | Get by document | USER, ADMIN |
| PATCH | `/v1/customers/{customerId}/status` | Change status | ADMIN |

### Validation

| Method | Path | Description | Role |
|---|---|---|---|
| POST | `/v1/customers/validate` | Validate by customerId or documentId | USER, ADMIN |
| GET | `/v1/customers/{customerId}/validate` | Validate by ID | USER, ADMIN |

### Observability

| Path | Description |
|---|---|
| `/q/health` | Health checks (liveness + readiness) |
| `/q/health/live` | Liveness check |
| `/q/health/ready` | Readiness check (includes DB) |
| `/q/metrics` | Prometheus metrics |

### Custom metrics

- `customer_operations_success_total` - Successful CRUD operations
- `customer_operations_failure_total` - Failed CRUD operations
- `customer_validation_success_total` - Successful validations
- `customer_validation_failure_total` - Failed validations
- `customer_active_total` - Gauge of active customers

## Usage examples

### Create a customer (ADMIN)

```bash
curl -X POST http://localhost:8080/v1/customers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{
    "firstName": "Juan",
    "lastName": "Perez",
    "documentType": "DNI",
    "documentId": "12345678",
    "email": "juan.perez@email.com",
    "phone": "+51987654321",
    "dateOfBirth": "1990-05-15",
    "address": "Av. Principal 123, Lima"
  }'
```

### List customers (USER or ADMIN)

```bash
curl http://localhost:8080/v1/customers?page=0&size=10&status=ACTIVE \
  -H "Authorization: Bearer <TOKEN_USER>"
```

### Get customer by ID

```bash
curl http://localhost:8080/v1/customers/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer <TOKEN_USER>"
```

### Update status (ADMIN)

```bash
curl -X PATCH http://localhost:8080/v1/customers/550e8400-e29b-41d4-a716-446655440000/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_ADMIN>" \
  -d '{
    "status": "ACTIVE",
    "reason": "KYC verification completed"
  }'
```

### Validate customer

```bash
curl -X POST http://localhost:8080/v1/customers/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_USER>" \
  -d '{
    "documentId": "12345678"
  }'
```

## Test credentials

### JWT users

| User | Role | Description |
|---|---|---|
| `admin1` | ROLE_ADMIN | Full access (CRUD + validation) |
| `user1` | ROLE_USER | Read and validation only |

### Database

| Parameter | Value |
|---|---|
| Host | localhost:5432 |
| Database | customer_db |
| User | customer_user |
| Password | customer_pass |

### Generate JWT token for testing

To generate test tokens, you can use the SmallRye JWT Build library included in the project, or tools like jwt.io with the RSA keys located in `src/main/resources/`.

**Payload for ADMIN:**
```json
{
  "iss": "https://challengebank.com",
  "sub": "admin1",
  "groups": ["ROLE_ADMIN"],
  "iat": 1700000000,
  "exp": 1900000000
}
```

**Payload for USER:**
```json
{
  "iss": "https://challengebank.com",
  "sub": "user1",
  "groups": ["ROLE_USER"],
  "iat": 1700000000,
  "exp": 1900000000
}
```

## Tests

### Run unit tests

```bash
./mvnw test
```

### Run tests with coverage report

```bash
./mvnw verify
```

The JaCoCo report is generated in `target/jacoco-report/`.

## Database structure

### Table: customers

| Column | Type | Constraint |
|---|---|---|
| customer_id | UUID | PK, auto-generated |
| first_name | VARCHAR(100) | NOT NULL |
| last_name | VARCHAR(100) | NOT NULL |
| document_type | VARCHAR(10) | NOT NULL (DNI, PASSPORT, CEDULA, RUC) |
| document_id | VARCHAR(20) | NOT NULL, UNIQUE with document_type |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| phone | VARCHAR(20) | Optional |
| date_of_birth | DATE | Optional |
| address | VARCHAR(500) | Optional |
| status | VARCHAR(10) | NOT NULL (ACTIVE, INACTIVE, SUSPENDED, PENDING) |
| created_at | TIMESTAMP | NOT NULL, auto-generated |
| updated_at | TIMESTAMP | Auto-updated |
