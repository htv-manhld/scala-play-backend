# 🏗️ DDD + Microservices Backend

Professional REST API backend with **Domain-Driven Design (DDD)** and **Microservices Architecture**, built with Scala 3.3.6 and Play Framework 3.0.9.

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
│   │   └── [Repository].scala   # Repository Interface
│   └── shared/          # Shared Domain Components
│       └── DomainBase.scala     # Base classes, Value Objects
│
├── application/         # Application Layer - Use cases
│   ├── [aggregate]/
│   │   ├── [Service].scala      # Application Service (Commands/Queries)
│   │   ├── [Commands].scala     # Commands & Queries definition
│   │   └── [DTO].scala          # Data Transfer Objects
│   └── shared/
│       └── ApplicationBase.scala # Application base classes
│
├── infrastructure/      # Infrastructure Layer - Technical details
│   ├── persistence/
│   │   └── [Repository]Impl.scala # Database implementation
│   ├── messaging/
│   │   ├── EventPublisherImpl.scala  # Event publishing
│   │   └── EventBusImpl.scala        # Event handling
│   └── external/        # External service integrations
│
├── interfaces/          # Interface Layer - Controllers
│   ├── rest/
│   │   ├── [Controller].scala    # REST endpoints
│   │   └── common/
│   │       └── ApiResponse.scala # API response wrapper
│   └── graphql/         # GraphQL endpoints (future)
│
└── modules/             # Dependency injection
    └── [Service]Module.scala    # DI bindings
```

### 🔄 Microservices Architecture

```
microservices/
├── api-gateway/        # Port 9000 - Routing, Auth, Rate Limiting
│   ├── Dockerfile
│   ├── build.sbt
│   ├── conf/application.conf
│   ├── .env
│   └── app/
│       ├── interfaces/rest/     # Gateway controllers
│       ├── application/shared/  # Cross-cutting concerns
│       └── infrastructure/external/  # Service integrations
│
├── user-service/       # Port 9001 - User Management with DDD
│   ├── Dockerfile
│   ├── build.sbt
│   ├── conf/application.conf
│   ├── .env
│   └── app/                     # Full DDD structure
│       ├── domain/user/         # User domain logic
│       ├── application/user/    # User use cases
│       ├── infrastructure/      # Database & messaging
│       ├── interfaces/rest/     # REST controllers
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
│       └── interfaces/rest/
│
└── analytics-service/    # Port 9003 - Metrics, Reporting
    ├── Dockerfile
    ├── build.sbt
    ├── .env
    └── app/                     # DDD structure for analytics
        ├── domain/analytics/
        ├── application/analytics/
        ├── infrastructure/
        └── interfaces/rest/
```

## 🚀 Quick Start

### 1. Setup Environment
```bash
# Setup environment files for all microservices
./scripts/setup-env.sh

# Edit each service's configuration:
# - microservices/api-gateway/.env
# - microservices/user-service/.env
# - microservices/notification-service/.env
# - microservices/analytics-service/.env

# Start application
./scripts/start.sh
```

### 2. Verify Services
```bash
# Check all services health
curl http://localhost:9000/health  # API Gateway
curl http://localhost:9001/health  # User Service
curl http://localhost:9002/health  # Notification Service
curl http://localhost:9003/health  # Analytics Service

# Test API endpoints
curl http://localhost:9000/api/users
```

## 🛠️ Technology Stack

### Core Services
- **API Gateway** (9000) - JWT Auth, Rate Limiting, Service Routing
- **User Service** (9001) - User domain với DDD patterns
- **Notification Service** (9002) - Email & Push notifications
- **Analytics Service** (9003) - Metrics & Reporting

### Infrastructure
- **PostgreSQL** (5432) - Primary database
- **Kafka + Zookeeper** (9092/2181) - Event streaming
- **Redis** (6379) - Caching & Sessions
- **Consul** (8500) - Service discovery
- **Prometheus** (9090) - Metrics collection
- **Grafana** (3000) - Monitoring dashboards

## 📊 DDD Patterns Implementation

### 🏗️ Tactical Patterns
- ✅ **Aggregate Root**: `User` aggregate với business rules
- ✅ **Value Objects**: `Email`, `UserProfile` với validation
- ✅ **Domain Events**: `UserCreated`, `UserProfileChanged`, `UserEmailChanged`, `UserDeactivated`
- ✅ **Repository Pattern**: Interface ở domain, implementation ở infrastructure
- ✅ **Domain Services**: `UserDomainService` cho complex business logic
- ✅ **Specifications**: Flexible business rules validation

### 🎯 Strategic Patterns
- ✅ **Bounded Contexts**: User Management, Notifications, Analytics
- ✅ **Context Mapping**: Service-to-service communication
- ✅ **Anti-Corruption Layer**: DTOs để isolate external concerns

## 🔄 Event-Driven Architecture

### Domain Events Flow
```
1. Domain Operation (e.g., User.changeEmail())
2. Domain Event Published (UserEmailChanged)
3. Event Handlers Process (Send verification email)
4. Cross-service Communication via Kafka
5. Analytics Service records metrics
```

### Event Examples
- `UserCreated` → Welcome email + Analytics tracking
- `UserProfileChanged` → Cache invalidation + Search index update
- `UserEmailChanged` → Email verification + External system sync
- `UserDeactivated` → Data cleanup + Service notifications

## 📋 API Endpoints

### User Management
```bash
# CRUD Operations
GET    /api/users              # List all users
GET    /api/users/{id}         # Get user by ID
GET    /api/users/by-email/{email}  # Get user by email
POST   /api/users              # Create new user
PUT    /api/users/{id}         # Update user profile
PUT    /api/users/{id}/email   # Change user email
DELETE /api/users/{id}         # Delete user

# Health & Status
GET    /api/health             # Service health check
```

### Request/Response Examples
```bash
# Create User
curl -X POST http://localhost:9000/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "age": 30
  }'

# Response
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
- Container health checks với automatic restart
- Service dependency validation

### Metrics & Logging
- **Prometheus** metrics at `/metrics` endpoints
- **Grafana** dashboards for visualization
- Structured logging với correlation IDs
- Distributed tracing headers

### Service Discovery
- **Consul** for service registration
- Automatic service discovery và load balancing
- Health-based routing

## 🧪 Testing Strategy

### Domain Layer Tests
```bash
# Test business logic in isolation
sbt "testOnly domain.user.*"
```

### Application Layer Tests
```bash
# Test use cases và workflows
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

### Service-Specific Environment Files

Each microservice has its own `.env` file for better separation:

**API Gateway** (`microservices/api-gateway/.env`):
```bash
APPLICATION_SECRET=your-64-char-secret-key
USER_SERVICE_URL=http://user-service:9001
NOTIFICATION_SERVICE_URL=http://notification-service:9002
ANALYTICS_SERVICE_URL=http://analytics-service:9003
JWT_SECRET=your-jwt-secret
RATE_LIMIT_REQUESTS_PER_MINUTE=100
```

**User Service** (`microservices/user-service/.env`):
```bash
APPLICATION_SECRET=your-64-char-secret-key
DB_DEFAULT_URL=jdbc:postgresql://postgres:5432/microservicesdb
DB_DEFAULT_USERNAME=postgres
DB_DEFAULT_PASSWORD=password
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

**Notification Service** (`microservices/notification-service/.env`):
```bash
APPLICATION_SECRET=your-64-char-secret-key
SMTP_HOST=smtp.gmail.com
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
EMAIL_RATE_LIMIT_PER_HOUR=100
```

**Analytics Service** (`microservices/analytics-service/.env`):
```bash
APPLICATION_SECRET=your-64-char-secret-key
DB_DEFAULT_URL=jdbc:postgresql://postgres:5432/microservicesdb
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
INFLUXDB_URL=http://influxdb:8086
METRICS_RETENTION_DAYS=30
```

### Setup Commands
```bash
# Auto-create all .env files from templates
./scripts/setup-env.sh

# Manual setup for specific service
cp microservices/user-service/.env.example microservices/user-service/.env
```

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
# Start development environment
./scripts/start.sh

# Build specific service
cd microservices/user-service && sbt compile

# Run tests
sbt test

# Build Docker images
docker compose build
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
```bash
# Access PostgreSQL
docker compose exec postgres psql -U postgres -d microservicesdb

# View tables
\dt

# Run migrations (if using Flyway)
docker compose exec user-service ./bin/user-service -Dflyway.migrate=true
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
# Verify database is running
docker compose ps postgres

# Check connection
docker compose exec postgres psql -U postgres -c "SELECT 1;"

# Update connection string in .env
DB_DEFAULT_URL=jdbc:postgresql://postgres:5432/microservicesdb
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

# Test message production
docker compose exec kafka kafka-console-producer --bootstrap-server localhost:9092 --topic user-domain-events
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

## 🎯 Roadmap & Future Enhancements

### Phase 2: Advanced Patterns
- [ ] **CQRS (Command Query Responsibility Segregation)**
- [ ] **Event Sourcing** cho audit trail
- [ ] **SAGA Pattern** cho distributed transactions
- [ ] **Circuit Breaker** pattern cho fault tolerance

### Phase 3: Production Readiness
- [ ] **API Versioning** với backward compatibility
- [ ] **Distributed Tracing** với Jaeger/Zipkin
- [ ] **Advanced Security** với OAuth2/OIDC
- [ ] **Database per Service** migration
- [ ] **Kubernetes Deployment** với Helm charts

### Phase 4: Observability & DevOps
- [ ] **Advanced Monitoring** với custom metrics
- [ ] **Log Aggregation** với ELK Stack
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