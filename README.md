# 🏗️ DDD + Microservices Backend

Professional REST API backend with **Domain-Driven Design (DDD)** and **Microservices Architecture**, built with Scala 3.7.3 and Play Framework 3.0.9.

## 📂 Project Structure

```
scala-play-backend/
├── scripts/
│   ├── start.sh              # One command to start everything
│   ├── setup-env.sh          # Generate .env files
│   └── merge-openapi.sh      # Bundle OpenAPI specs
│
└── microservices/
    ├── api-gateway/          # Port 9000 - Single Entry Point
    │   ├── openapi/
    │   │   ├── openapi.yaml           # Aggregated spec (all services)
    │   │   └── openapi-bundled.yaml   # Single-file bundle
    │   └── app/
    │       └── interfaces/rest/       # Gateway controllers & routing
    │
    ├── user-service/         # Port 9001 - User Management (DDD Score: 92/100)
    │   ├── openapi/
    │   │   ├── openapi.yaml           # Main spec with $ref
    │   │   ├── paths/                 # API endpoints
    │   │   └── components/            # Schemas, responses, parameters
    │   └── app/
    │       ├── domain/                # Domain Layer - Pure business logic
    │       │   ├── user/
    │       │   │   ├── User.scala             # Aggregate Root (immutable + events)
    │       │   │   ├── UserId.scala           # Value Object (sealed trait)
    │       │   │   ├── Email.scala            # Value Object (validated)
    │       │   │   ├── UserProfile.scala      # Value Object
    │       │   │   ├── UserStatus.scala       # Enum
    │       │   │   ├── UserRepository.scala   # Repository Interface
    │       │   │   ├── UserDomainService.scala # Domain Service
    │       │   │   ├── UserFactory.scala      # Factory Pattern
    │       │   │   ├── PasswordHasher.scala   # Domain Interface
    │       │   │   ├── specifications/        # Specification Pattern
    │       │   │   │   └── UserSpecification.scala
    │       │   │   └── events/               # Domain Events
    │       │   │       ├── UserCreated.scala
    │       │   │       ├── UserProfileChanged.scala
    │       │   │       └── UserEmailChanged.scala
    │       │   └── shared/            # Shared domain primitives
    │       │       ├── DomainBase.scala      # AggregateRoot, DomainEvent
    │       │       └── DomainError.scala
    │       ├── application/           # Application Layer - Use cases & orchestration
    │       │   ├── user/
    │       │   │   ├── UserService.scala     # Application Service
    │       │   │   ├── commands/            # CQRS Commands (write)
    │       │   │   │   ├── CreateUserCommand.scala
    │       │   │   │   ├── UpdateUserProfileCommand.scala
    │       │   │   │   ├── ChangeUserEmailCommand.scala
    │       │   │   │   └── DeleteUserCommand.scala
    │       │   │   └── queries/             # CQRS Queries (read)
    │       │   │       ├── GetUserByIdQuery.scala
    │       │   │       ├── GetUserByEmailQuery.scala
    │       │   │       ├── GetAllUsersQuery.scala
    │       │   │       └── GetUsersPaginatedQuery.scala
    │       │   └── shared/            # Shared application logic
    │       │       ├── ApplicationBase.scala
    │       │       └── LoggingService.scala
    │       ├── infrastructure/        # Infrastructure Layer - Technical implementation
    │       │   ├── persistence/
    │       │   │   └── UserRepositoryImpl.scala  # Slick + PostgreSQL
    │       │   ├── messaging/
    │       │   │   ├── EventPublisher.scala
    │       │   │   ├── KafkaEventPublisher.scala # Kafka integration
    │       │   │   └── EventSubscriber.scala
    │       │   └── security/
    │       │       └── BCryptPasswordHasher.scala # Password hashing impl
    │       ├── controllers/           # Interface Layer - REST API
    │       │   ├── UserController.scala
    │       │   ├── HealthController.scala
    │       │   ├── MetricsController.scala
    │       │   └── dto/               # Request/Response DTOs
    │       │       └── UserDto.scala
    │       └── modules/               # Dependency Injection (Guice)
    │           ├── UserServiceModule.scala
    │           ├── PrometheusModule.scala
    │           └── ConsulModule.scala
    │
    ├── notification-service/ # Port 9002 - Email, Push notifications
    └── analytics-service/    # Port 9003 - Metrics, Reporting
```

### DDD Layers (Each Microservice)

```
app/
├── domain/                      # Domain Layer - Pure business logic
│   ├── [aggregate]/             # Aggregate Root + Value Objects
│   │   ├── [Aggregate].scala           # Aggregate Root (immutable + events)
│   │   ├── [Aggregate]Id.scala         # Entity ID (sealed trait pattern)
│   │   ├── [ValueObject].scala         # Value Objects (self-validating)
│   │   ├── [Aggregate]Repository.scala # Repository Interface (in domain!)
│   │   ├── [Aggregate]DomainService.scala # Domain Service
│   │   ├── [Aggregate]Factory.scala    # Factory Pattern
│   │   ├── specifications/             # Specification Pattern
│   │   │   └── [Aggregate]Specification.scala
│   │   └── events/                     # Domain Events
│   │       ├── [Event1].scala
│   │       └── [Event2].scala
│   └── shared/                  # Shared domain primitives
│       ├── DomainBase.scala     # AggregateRoot, DomainEvent traits
│       └── DomainError.scala    # Domain error types
│
├── application/                 # Application Layer - Use cases & orchestration
│   ├── [aggregate]/
│   │   ├── [Aggregate]Service.scala    # Application Service (orchestration)
│   │   ├── commands/            # CQRS Commands (write operations)
│   │   │   ├── Create[Aggregate]Command.scala
│   │   │   ├── Update[Aggregate]Command.scala
│   │   │   └── Delete[Aggregate]Command.scala
│   │   └── queries/             # CQRS Queries (read operations)
│   │       ├── Get[Aggregate]ByIdQuery.scala
│   │       └── GetAll[Aggregate]sQuery.scala
│   └── shared/                  # Shared application logic
│       ├── ApplicationBase.scala
│       └── LoggingService.scala
│
├── infrastructure/              # Infrastructure Layer - Technical implementation
│   ├── persistence/             # Database implementations
│   │   └── [Aggregate]RepositoryImpl.scala  # Repository implementation
│   ├── messaging/               # Event publishing
│   │   ├── EventPublisher.scala
│   │   └── KafkaEventPublisher.scala
│   └── security/                # Security implementations
│       └── BCryptPasswordHasher.scala
│
├── controllers/                 # Interface Layer - REST API
│   ├── [Aggregate]Controller.scala
│   ├── HealthController.scala
│   └── dto/                     # Request/Response DTOs
│       └── [Aggregate]Dto.scala
│
└── modules/                     # Dependency Injection (Guice)
    └── [Aggregate]ServiceModule.scala
```

**DDD Patterns Implemented:**
- ✅ **Aggregate Root** - Immutable with domain events
- ✅ **Value Objects** - Self-validating, sealed traits for IDs
- ✅ **Domain Services** - Complex business logic
- ✅ **Factory Pattern** - Centralized creation & validation
- ✅ **Specification Pattern** - Composable business rules
- ✅ **Repository Pattern** - Interface in domain, impl in infrastructure
- ✅ **Domain Events** - Event sourcing pattern
- ✅ **CQRS** - Separate read/write models

## 🛠️ Technology Stack

### Backend
- **Language**: Scala 3.7.3
- **Framework**: Play Framework 3.0.9
- **Architecture**: DDD + Microservices + Event-Driven
- **API**: OpenAPI 3.1 specs

### Infrastructure
- **Databases**: PostgreSQL (separate DB per service)
  - user-service: :5432
  - notification-service: :5433
  - analytics-service: :5434
- **Message Broker**: Kafka + Zookeeper
- **Cache**: Redis
- **Service Discovery**: Consul
- **Monitoring**:
  - **Prometheus** - Metrics collection & querying
  - **Grafana** - Metrics visualization & dashboards
  - **Consul** - Service registry & health checks
- **Container**: Docker + Docker Compose

### Key Patterns
- **API Gateway** - Single entry point (port 9000)
- **Database per Service** - Isolated databases
- **Event-Driven** - Kafka for service communication
- **CQRS** - Separate read/write operations
- **Observability** - Metrics, health checks, service discovery

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- Ports available: 9000-9003, 5432-5434, 9092, 6379, 8500, 3000, 9090

### Quick Start

```bash
# 1. Start everything (auto setup .env files)
./scripts/start.sh

# 2. Verify services
curl http://localhost:9000/health

# 3. Test API
curl -X POST http://localhost:9000/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","birthdate":"1990-01-15"}'
```

### Manual Setup (Optional)

```bash
# Generate .env files
./scripts/setup-env.sh

# Start infrastructure only
docker compose up -d user-db notification-db analytics-db kafka redis consul

# Start specific service
docker compose up -d user-service

# View logs
docker compose logs -f user-service

# Stop all
docker compose down
```

## 📋 API Documentation

### OpenAPI Specs

```bash
# Merge all service specs and bundle
./scripts/merge-openapi.sh

# View in Swagger Editor
open https://editor.swagger.io
# Upload: microservices/api-gateway/openapi/openapi-bundled.yaml
```

### API Endpoints (via Gateway :9000)

**User Management:**
```bash
GET    /api/users                     # List users
GET    /api/users/paginated           # Paginated list
GET    /api/users/{id}                # Get by ID
POST   /api/users                     # Create user
PUT    /api/users/{id}                # Update user
DELETE /api/users/{id}                # Delete user
```

**Analytics:**
```bash
GET    /api/analytics/metrics/system  # System metrics
```

### Example Request/Response

```bash
# Create user
curl -X POST http://localhost:9000/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "birthdate": "1990-01-15"
  }'

# Response
{
  "success": true,
  "data": {
    "id": "1",
    "email": "john@example.com",
    "name": "John Doe",
    "birthdate": "1990-01-15",
    "status": 1,
    "createdAt": "2025-10-03T10:30:00",
    "updatedAt": "2025-10-03T10:30:00"
  },
  "message": "User created successfully"
}
```

## 🔧 Development

### Build & Run

```bash
# Rebuild service after code changes
docker compose build user-service
docker compose up -d user-service

# Run tests
docker compose exec user-service sbt test

# Test specific layers
docker compose exec user-service sbt "testOnly domain.user.*"
docker compose exec user-service sbt "testOnly application.user.*"
```

### Database

```bash
# Run migrations
docker exec microservices-user-service migrate

# Access database
docker compose exec user-db psql -U postgres -d userdb

# Query users
docker compose exec user-db psql -U postgres -d userdb -c "SELECT * FROM users;"
```

### Debugging

```bash
# Check services
docker compose ps

# View logs
docker compose logs -f user-service

# Monitor Kafka events
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic domain-events \
  --from-beginning
```

## 📊 Monitoring & Observability

All microservices include built-in monitoring with:
- **Consul** (http://localhost:8500) - Service discovery & health checks
- **Prometheus** (http://localhost:9090) - Metrics collection
- **Grafana** (http://localhost:3000) - Dashboards & visualization

### Monitoring Architecture

```
                    ┌─────────────┐
                    │   Grafana   │  ← Dashboards & Alerts
                    │   :3000     │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Prometheus  │  ← Metrics Database
                    │   :9090     │
                    └──────┬──────┘
                           │ (scrapes /metrics every 15s)
        ┌──────────────────┼──────────────────┬──────────────┐
        │                  │                  │              │
   ┌────▼────┐        ┌────▼────┐        ┌────▼────┐    ┌────▼────┐
   │ Gateway │        │  User   │        │ Notif.  │    │Analytics│
   │  :9000  │        │ Service │        │ Service │    │ Service │
   │ /health │        │  :9001  │        │  :9002  │    │  :9003  │
   │ /metrics│        │ /metrics│        │ /metrics│    │ /metrics│
   └────┬────┘        └────┬────┘        └────┬────┘    └────┬────┘
        │                  │                  │              │
        └──────────────────┼──────────────────┴──────────────┘
                           │ (health checks & service discovery)
                     ┌─────▼─────┐
                     │   Consul  │  ← Service Registry
                     │   :8500   │
                     └───────────┘
```

### Available Metrics

Each service automatically exposes:
- **Health**: `/health` endpoint for health checks
- **Metrics**: `/metrics` endpoint in Prometheus format
  - JVM memory (heap/non-heap)
  - Thread count & states
  - Garbage collection stats
  - Class loading

**📘 Full Documentation: [common-monitoring/README.md](microservices/common-monitoring/README.md)**
- Complete setup guide
- Browser testing instructions
- Prometheus queries & Grafana dashboards
- Troubleshooting & adding monitoring to new services

## 📊 Architecture Flow

```
Client → API Gateway :9000 → Microservices
             ↓
  ┌──────────┼──────────┐
  ↓          ↓          ↓
User    Notification Analytics
:9001      :9002      :9003
  ↓          ↓          ↓
PostgreSQL  PostgreSQL  PostgreSQL
  ↓          ↑          ↑
  └──→ Kafka Events ────┘
```

**Event Flow:**
1. User creates account → `POST /api/users`
2. User Service publishes `UserCreated` event to Kafka
3. Notification Service consumes event → Sends welcome email
4. Analytics Service consumes event → Increments user counter

---

**Built with ❤️ using Domain-Driven Design + Microservices Architecture**
