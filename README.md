# 🏗️ DDD + Microservices Backend

Professional REST API backend with **Domain-Driven Design (DDD)** and **Microservices Architecture**, built with Scala 3.7.3 and Play Framework 3.0.9.

## 📋 Architecture Overview

### 🎯 Current Architecture Structure

**Main App** (Legacy Redirector):
```
app/
├── controllers/        # Simple redirect controllers
│   └── RedirectController.scala    # Redirects to microservices info
└── views/             # Basic HTML templates
    └── index.scala.html            # Microservices overview page
```

**Each Microservice** follows full DDD structure:
```
microservices/[service-name]/app/
├── domain/              # Domain Layer - Pure business logic
│   ├── [aggregate]/     # Domain Aggregates (e.g., user/)
│   │   ├── [Entity].scala       # Aggregate Root with business rules
│   │   ├── [Repository].scala   # Repository Interface
│   │   └── events/              # Domain Events
│   └── shared/          # Shared Domain Components
│       └── DomainBase.scala     # Base classes, Value Objects
│
├── application/         # Application Layer - Use cases
│   ├── [aggregate]/
│   │   ├── commands/    # Command handlers (write operations)
│   │   ├── queries/     # Query handlers (read operations)
│   │   └── [Service].scala      # Application Service
│   └── shared/
│       └── ApplicationBase.scala # Application base classes
│
├── infrastructure/      # Infrastructure Layer - Technical details
│   ├── persistence/
│   │   └── [Repository]Impl.scala # Database implementation
│   ├── messaging/
│   │   ├── KafkaEventPublisher.scala  # Event publishing
│   │   └── EventSubscriber.scala      # Event consuming
│   └── external/        # External service integrations
│
├── controllers/         # Interface Layer - REST endpoints
│   ├── [Controller].scala    # HTTP controllers
│   └── dto/             # Data Transfer Objects
│
└── modules/             # Dependency injection
    └── [Service]Module.scala    # DI bindings
```

### 🔄 Microservices Architecture

**API Gateway Pattern**: All client requests go through API Gateway (port 9000), providing a single entry point with routing, authentication, rate limiting, and load balancing.

```
Client Requests → API Gateway :9000 → Backend Services
                       ↓
            ┌──────────┼──────────┐
            ↓          ↓          ↓
      User Service  Notification  Analytics
         :9001        :9002        :9003
```

```
microservices/
├── api-gateway/        # Port 9000 - Single Entry Point
│   ├── Dockerfile
│   ├── build.sbt
│   ├── conf/application.conf
│   ├── .env
│   └── app/
│       ├── interfaces/rest/     # Gateway controllers & routing
│       ├── application/shared/  # Cross-cutting concerns (Auth, Rate Limiting)
│       └── infrastructure/external/  # Service integrations & proxying
│
├── user-service/       # Port 9001 - User Management with DDD
│   ├── Dockerfile
│   ├── build.sbt
│   ├── conf/application.conf
│   ├── .env
│   └── app/                     # Full DDD structure
│       ├── domain/user/         # User domain logic & events
│       ├── application/user/    # Commands, Queries, Service
│       ├── infrastructure/      # Persistence & Kafka messaging
│       ├── controllers/         # REST controllers & DTOs
│       └── modules/             # Dependency injection
│
├── notification-service/ # Port 9002 - Email, Push notifications
│   ├── Dockerfile
│   ├── build.sbt
│   ├── .env
│   └── app/                     # DDD structure for notifications
│       ├── domain/notification/
│       ├── application/notification/
│       ├── infrastructure/
│       └── controllers/
│
└── analytics-service/    # Port 9003 - Metrics, Reporting
    ├── Dockerfile
    ├── build.sbt
    ├── .env
    └── app/                     # DDD structure for analytics
        ├── domain/analytics/
        ├── application/analytics/
        ├── infrastructure/
        └── controllers/
```

## 🚀 Quick Start

### Prerequisites
- Docker & Docker Compose installed
- Ports 9000-9003, 5432, 9092, 6379, 8500, 3000, 9090 available

### Start Application
```bash
# One command to start everything (with auto .env setup)
./scripts/start.sh
```

Script will automatically:
- ✅ Create `.env` files from examples if missing
- ✅ Start infrastructure (PostgreSQL, Kafka, Redis, Consul, etc.)
- ✅ Start all microservices
- ✅ Check health of all services

### Verify Services
```bash
# Check API Gateway health
curl http://localhost:9000/health

# All requests go through API Gateway (port 9000)
# Gateway routes to backend services automatically

# Run database migrations
docker exec microservices-user-service migrate

# Test the full event-driven flow via API Gateway
curl -X POST http://localhost:9000/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","age":30}'

# Check analytics via API Gateway (totalUsers should increment)
curl http://localhost:9000/api/analytics/metrics/system

# Direct service access (for debugging only)
curl http://localhost:9001/health  # User Service
curl http://localhost:9002/health  # Notification Service
curl http://localhost:9003/health  # Analytics Service
```

**Note:** Migrations auto-run on startup, but you can manually trigger with `docker exec microservices-user-service migrate`

### Optional: Manual Environment Setup
```bash
# If you need to customize configs before starting
./scripts/setup-env.sh

# Then edit .env files as needed
nano microservices/notification-service/.env  # Update SMTP settings
```

## 🛠️ Technology Stack

### Core Services
- **API Gateway** (9000) - **Single Entry Point**
  - Routing: Route requests to backend services
  - Authentication: JWT validation & token management
  - Rate Limiting: Throttling per client/endpoint
  - Load Balancing: Distribute traffic across service instances
  - Request/Response transformation
- **User Service** (9001) - User domain with DDD patterns
- **Notification Service** (9002) - Email & Push notifications
- **Analytics Service** (9003) - Metrics & Reporting

### Infrastructure
- **PostgreSQL Databases** - Separate database per service
  - User DB (5432) - userdb
  - Notification DB (5433) - notificationdb
  - Analytics DB (5434) - analyticsdb
- **Kafka + Zookeeper** (9092/2181) - Event streaming
- **Redis** (6379) - Caching & Sessions
- **Consul** (8500) - Service discovery
- **Prometheus** (9090) - Metrics collection
- **Grafana** (3000) - Monitoring dashboards

## 🚪 API Gateway Pattern

### Why Use API Gateway?

**Before API Gateway:**
```
Mobile App  ──→ User Service :9001
Web App     ──→ Notification Service :9002
Desktop App ──→ Analytics Service :9003
```
❌ Clients must know all service endpoints
❌ Duplicate authentication logic in each service
❌ Hard to manage CORS, rate limiting
❌ No centralized logging

**After API Gateway:**
```
Mobile App  ─┐
Web App     ─┼──→ API Gateway :9000 ──→ Backend Services
Desktop App ─┘
```
✅ Single entry point for all clients
✅ Centralized authentication & authorization
✅ Unified rate limiting & CORS policies
✅ Centralized request logging & monitoring
✅ Automatic service discovery & load balancing

### Request Flow Through API Gateway

```
1. Client → API Gateway :9000
   POST /api/users
   Headers: Authorization: Bearer <token>

2. API Gateway processing:
   ├─ Validate JWT token
   ├─ Check rate limit
   ├─ Log request
   └─ Route to User Service

3. API Gateway → User Service :9001
   POST /api/users
   Headers: X-User-Id, X-Request-Id

4. User Service processes → Response

5. API Gateway transforms response → Client
   {success: true, data: {...}}
```

### Gateway Features

**Authentication & Authorization:**
- JWT token validation
- User role/permission checks
- Token refresh mechanism

**Traffic Management:**
- Rate limiting per client/endpoint
- Request throttling
- Load balancing across service instances

**Request Transformation:**
- Add correlation IDs for tracing
- Inject user context headers
- Format standardization

**Observability:**
- Centralized request logging
- Performance metrics per endpoint
- Error tracking & alerting

## 📊 DDD Patterns Implementation

### 🏗️ Tactical Patterns
- ✅ **Aggregate Root**: `User` aggregate with business rules
- ✅ **Value Objects**: `Email`, `UserProfile` with validation
- ✅ **Domain Events**: `UserCreated`, `UserProfileChanged`, `UserEmailChanged`, `UserDeactivated`
- ✅ **Repository Pattern**: Interface in domain, implementation in infrastructure
- ✅ **Domain Services**: `UserDomainService` for complex business logic
- ✅ **Specifications**: Flexible business rules validation

### 🎯 Strategic Patterns
- ✅ **Bounded Contexts**: User Management, Notifications, Analytics
- ✅ **Context Mapping**: Service-to-service communication
- ✅ **Anti-Corruption Layer**: DTOs to isolate external concerns

## 🔄 Event-Driven Architecture

### Domain Events Flow
```
1. User creates account via POST /api/users
2. UserService.createUser() → User aggregate created
3. User.uncommittedEvents contains UserCreated event
4. KafkaEventPublisher publishes to "domain-events" topic (kafka:29092)
5. Notification Service consumes event → Sends welcome email
6. Analytics Service consumes event → Increments totalUsers counter
```

### Kafka Configuration
- **Topic**: `domain-events` (single topic for all domain events)
- **Internal listener**: `kafka:29092` (for containers)
- **External listener**: `localhost:9092` (for host machine)
- **Event format**: JSON with eventType as key

### Event Examples
- `UserCreated` → Welcome email + Analytics tracking
- `UserProfileChanged` → Cache invalidation + Search index update
- `UserEmailChanged` → Email verification + External system sync
- `UserDeactivated` → Data cleanup + Service notifications

### Testing Event Flow
```bash
# Create a user
curl -X POST http://localhost:9001/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","age":25}'

# Check analytics (should increment totalUsers)
curl http://localhost:9003/api/analytics/metrics/system

# Monitor Kafka events in real-time
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic domain-events \
  --from-beginning
```

## 📋 API Endpoints

**Important**: All API requests must go through the **API Gateway** at `http://localhost:9000`

### API Gateway Routing Rules
```
GET/POST  /api/users/*           → User Service (9001)
GET/POST  /api/notifications/*   → Notification Service (9002)
GET/POST  /api/analytics/*       → Analytics Service (9003)
GET       /health                → API Gateway health
```

### User Management (via API Gateway)
```bash
# Base URL: http://localhost:9000

# CRUD Operations
GET    /api/users                     # List all users
GET    /api/users/{id}                # Get user by ID
GET    /api/users/by-email/{email}    # Get user by email
POST   /api/users                     # Create new user
PUT    /api/users/{id}                # Update user profile
PUT    /api/users/{id}/email          # Change user email
DELETE /api/users/{id}                # Delete user

# Analytics (via API Gateway)
GET    /api/analytics/metrics/system  # System metrics

# Health & Status
GET    /health                        # API Gateway health
```

### Request/Response Examples
```bash
# Create User (via API Gateway)
curl -X POST http://localhost:9000/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "age": 30
  }'

# Get All Users (via API Gateway)
curl http://localhost:9000/api/users

# Get Analytics (via API Gateway)
curl http://localhost:9000/api/analytics/metrics/system

# Response Format
{
  "success": true,
  "data": {"id": 1},
  "message": "User created successfully",
  "timestamp": "2025-09-24T10:30:00Z"
}
```

## 🔍 Monitoring & Observability

### Health Checks
- All services expose `/health` endpoint
- Container health checks with automatic restart
- Service dependency validation

### Metrics & Logging
- **Prometheus** metrics at `/metrics` endpoints
- **Grafana** dashboards for visualization
- Structured logging with correlation IDs
- Distributed tracing headers

### Service Discovery
- **Consul** for service registration
- Automatic service discovery and load balancing
- Health-based routing

## 🧪 Testing Strategy

### Domain Layer Tests
```bash
# Test business logic in isolation
sbt "testOnly domain.user.*"
```

### Application Layer Tests
```bash
# Test use cases and workflows
sbt "testOnly application.user.*"
```

### Integration Tests
```bash
# Test full request/response cycle
sbt "testOnly interfaces.rest.*"
```

### Docker Testing
```bash
# Test entire microservices stack
docker compose up -d
# Run API tests against running services
./scripts/test-api.sh
```

## ⚙️ Configuration

### Environment Setup

All services can be configured via `.env` files with sensible defaults in `application.conf`.

**Quick Setup:**
```bash
# Auto-generate all .env files from examples
./scripts/setup-env.sh

# Or manually copy for specific services
cp microservices/user-service/.env.example microservices/user-service/.env
cp microservices/notification-service/.env.example microservices/notification-service/.env
cp microservices/analytics-service/.env.example microservices/analytics-service/.env
```

**For Production:**
- Update `APPLICATION_SECRET` (must be 64+ characters)
- Configure SMTP settings in notification-service `.env`
- Update database credentials
- See `.env.example` files for all available options

**Configuration Priority:**
1. Environment variables from `.env` (highest priority)
2. Default values in `application.conf` (fallback)

## 🚀 Deployment

### Development
```bash
# Start all services
./scripts/start.sh

# Scale specific service
docker compose up -d --scale user-service=3

# View logs
docker compose logs -f user-service
```

### Production (Kubernetes Example)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: user-service
  template:
    spec:
      containers:
      - name: user-service
        image: user-service:1.0.0
        ports:
        - containerPort: 9001
        env:
        - name: DB_DEFAULT_URL
          valueFrom:
            configMapKeyRef:
              name: db-config
              key: url
        livenessProbe:
          httpGet:
            path: /health
            port: 9001
          initialDelaySeconds: 30
```

## 🔧 Development Commands

### Build & Run
```bash
# Start everything (recommended)
./scripts/start.sh

# Or manual control
docker compose up -d                    # Start all services
docker compose up -d user-service       # Start specific service
docker compose down                     # Stop all services
docker compose restart user-service     # Restart specific service

# Rebuild after code changes
docker compose build user-service
docker compose up -d user-service

# Run tests (inside container)
docker compose exec user-service sbt test
```

### Debugging
```bash
# Check service status
docker compose ps

# View specific service logs
docker compose logs -f user-service

# Access service shell
docker compose exec user-service bash

# Monitor resource usage
docker compose top
```

### Database Operations

#### Migrations (Play Evolutions)
```bash
# Run migrations manually
docker exec microservices-user-service migrate

# Migrations auto run when start service (autoApply=true in application.conf)
# Migration files: microservices/user-service/conf/evolutions/default/*.sql
```

#### Database Access
```bash
# Access separate databases per service
docker compose exec user-db psql -U postgres -d userdb
docker compose exec notification-db psql -U postgres -d notificationdb
docker compose exec analytics-db psql -U postgres -d analyticsdb

# View tables in user database
docker compose exec user-db psql -U postgres -d userdb -c "\dt"

# Query users
docker compose exec user-db psql -U postgres -d userdb -c "SELECT * FROM users;"

# Connect with DBeaver/pgAdmin:
# User DB:         localhost:5432 (userdb)
# Notification DB: localhost:5433 (notificationdb)
# Analytics DB:    localhost:5434 (analyticsdb)
# All use postgres/password
```

## 📈 Performance & Scaling

### Horizontal Scaling
```bash
# Scale services independently
docker compose up -d --scale user-service=3
docker compose up -d --scale notification-service=2

# Load balancing via API Gateway
# Automatic with Consul service discovery
```

### Caching Strategy
- **Redis** for session storage and API responses
- **Application-level** caching in services
- **Database query** caching with proper TTL

### Database Optimization
- **Connection pooling** with HikariCP
- **Read replicas** for analytics queries
- **Database sharding** when needed

## 🆘 Troubleshooting

### Common Issues

**Services not starting?**
```bash
# Check Docker daemon
docker info

# Check port conflicts
netstat -tulpn | grep :9000

# Restart infrastructure
docker compose down
./scripts/start.sh
```

**Database connection failed?**
```bash
# Verify databases are running
docker compose ps user-db notification-db analytics-db

# Check connections
docker compose exec user-db psql -U postgres -d userdb -c "SELECT 1;"
docker compose exec notification-db psql -U postgres -d notificationdb -c "SELECT 1;"
docker compose exec analytics-db psql -U postgres -d analyticsdb -c "SELECT 1;"

# Connection strings in .env:
# user-service:        DB_URL=jdbc:postgresql://user-db:5432/userdb
# notification-service: DB_URL=jdbc:postgresql://notification-db:5432/notificationdb
# analytics-service:    DB_URL=jdbc:postgresql://analytics-db:5432/analyticsdb
```

**Service discovery issues?**
```bash
# Check Consul
curl http://localhost:8500/v1/agent/services

# Verify service registration
docker compose logs consul
```

**Kafka not working?**
```bash
# Check Kafka status
docker compose logs kafka

# List topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume domain events (test event flow)
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic domain-events \
  --from-beginning

# Verify Kafka listeners
# Internal (containers): kafka:29092
# External (host): localhost:9092
```

**Configuration errors?**
```bash
# Run setup script to generate all .env files
./scripts/setup-env.sh

# Rebuild after config changes
docker compose build user-service notification-service analytics-service

# Verify environment variables
docker compose exec user-service env | grep APPLICATION_SECRET
```

**Services not responding after config changes?**
```bash
# Always rebuild images after configuration changes
docker compose build user-service notification-service analytics-service

# Then restart the services
docker compose up -d user-service notification-service analytics-service

# Verify services are healthy
curl http://localhost:9001/health  # User service
curl http://localhost:9002/health  # Notification service
curl http://localhost:9003/health  # Analytics service
```

**Analytics service showing totalUsers: 0?**
```bash
# This usually means events aren't being processed
# Check that EventSubscriber is awaiting the handler Future

# Verify events are being published
docker compose logs user-service | grep "Event published"

# Verify events are being consumed
docker compose logs analytics-service | grep "Analytics"

# Expected output:
# [Analytics] UserCreated tracked - UserID: X, Total users: Y
```

## 📚 Key Benefits Achieved

### DDD Benefits
- ✅ **Business Logic Isolation**: Domain logic separated from technical concerns
- ✅ **Rich Domain Model**: Entities with behaviors, not just data containers
- ✅ **Testability**: Domain logic can be tested without external dependencies
- ✅ **Maintainability**: Clear boundaries and single responsibility

### Microservices Benefits
- ✅ **Independent Scaling**: Scale each service according to demand
- ✅ **Technology Diversity**: Each service can use different tech stack
- ✅ **Fault Isolation**: Error in one service doesn't crash entire system
- ✅ **Team Autonomy**: Teams develop/deploy independently
- ✅ **Continuous Deployment**: Deploy each service without affecting others
- ✅ **Database per Service**: Each microservice has its own isolated database

## 🎯 Roadmap & Future Enhancements

### Phase 2: Advanced Patterns
- [ ] **CQRS (Command Query Responsibility Segregation)**
- [ ] **Event Sourcing** for audit trail
- [ ] **SAGA Pattern** for distributed transactions
- [ ] **Circuit Breaker** pattern for fault tolerance

### Phase 3: Production Readiness
- [x] **API Gateway** - ✅ Implemented (routing, auth, rate limiting)
- [ ] **API Versioning** with backward compatibility
- [ ] **Distributed Tracing** with Jaeger/Zipkin
- [ ] **Advanced Security** with OAuth2/OIDC
- [x] **Database per Service** - ✅ Implemented (separate DBs for each service)
- [ ] **Kubernetes Deployment** with Helm charts

### Phase 4: Observability & DevOps
- [ ] **Advanced Monitoring** with custom metrics
- [ ] **Log Aggregation** with ELK Stack
- [ ] **Automated Testing** pipeline
- [ ] **Blue-Green Deployment**
- [ ] **Chaos Engineering** testing

---

## 🤝 Contributing

1. Follow DDD principles and keep domain logic pure
2. Write tests for domain logic first
3. Use conventional commit messages
4. Update API documentation for changes
5. Test with full microservices stack before commit

**Built with ❤️ using Domain-Driven Design + Microservices Architecture**
