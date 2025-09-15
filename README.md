# Scala Play Backend API

Professional REST API backend built with Scala 3.3.6 LTS and Play Framework 3.0.9, featuring PostgreSQL database integration and Flyway migrations.

## Project Structure

```
├─ build.sbt                # Build configuration with Flyway plugin
├─ conf/
│   ├─ application.conf     # Configuration (DB, CORS, Flyway)
│   ├─ logback.xml         # Logging configuration
│   └─ routes              # API route definitions
├─ app/
│   ├─ controllers/        # Controllers organized by domain
│   │    ├─ user/          # User-related controllers
│   │    ├─ admin/         # Admin controllers
│   │    └─ api/           # API controllers
│   ├─ models/             # Data models
│   │    ├─ domain/        # Domain models & DTOs
│   │    └─ persistence/   # Database entities & repositories
│   ├─ services/           # Business logic services
│   ├─ modules/            # Guice dependency injection modules
│   └─ utils/              # Helper utilities
├─ src/main/resources/     # Resources
│   └─ db/migration/       # Flyway database migrations
├─ test/                   # Unit and integration tests
├─ public/                 # Static assets (minimal for API)
└─ project/                # SBT build configuration
```

## Features

- ✅ **RESTful API** with standardized JSON responses
- ✅ **Database Integration** with PostgreSQL and Slick ORM
- ✅ **Migration System** using Flyway for version control
- ✅ **Dependency Injection** with Google Guice
- ✅ **CORS Support** for cross-origin requests
- ✅ **Docker Ready** with PostgreSQL containerization
- ✅ **Layered Architecture** with domain separation
- ✅ **Scala 3 Compatibility** with latest language features
- ✅ **Error Handling** with proper HTTP status codes
- ✅ **Logging Configuration** with Logback
- ✅ **Testing Ready** with ScalaTest integration

## Technologies

- **Scala 3.3.6 LTS** - Latest stable LTS version
- **Play Framework 3.0.9** - Latest stable web framework
- **PostgreSQL** with Slick ORM - Database
- **Flyway** - Database migrations
- **Guice** - Dependency injection
- **ScalaTest** - Testing framework
- **Docker** - Containerization

## Recent Updates

### Version 2.0 (September 2025)
- ⬆️ **Upgraded to Scala 3.3.6 LTS** - Latest stable Long-Term Support version
- ⬆️ **Upgraded to Play Framework 3.0.9** - Latest stable release with Pekko integration
- 🔧 **Updated Dependencies** - All libraries compatible with Scala 3 and Play 3.0.x
- 🛠️ **Code Modernization** - Fixed Scala 3 compatibility issues in controllers
- ✅ **Full Compatibility** - Tested and verified with Java 21

## Getting Started

### Prerequisites
- **Java 21+** (OpenJDK recommended)
- **SBT 1.x** (Scala Build Tool)
- **Docker & Docker Compose** (for PostgreSQL)

### Java 21 Setup (Important!)
This project requires Java 21 and uses Scala 3.3.6 LTS with Play Framework 3.0.9. If you have multiple Java versions:

```bash
# Check current Java version
java -version

# If not Java 21, use the run script which sets Java 21 automatically
./run.sh
```

The project includes:
- `run.sh` script that automatically uses Java 21
- `build.sbt` configured for Java 21 compilation and Scala 3.3.6 LTS
- Updated dependencies compatible with Play Framework 3.0.9

### Quick Start

1. **Start PostgreSQL database:**
   ```bash
   docker compose up -d
   ```

2. **Start the application (choose one):**
   ```bash
   # Option 1: Using sbt directly (requires Java 21 as default)
   sbt run

   # Option 2: Using run script (guaranteed Java 21)
   ./run.sh
   ```

   Access: http://localhost:9000

### Database Migrations (Optional)
```bash
# Run migrations if needed
sbt flywayMigrate

# Check migration status
sbt flywayInfo
```

## API Endpoints

### User Management
- `GET /api/users` - Get all users
- `GET /api/users/:id` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/:id` - Update user
- `DELETE /api/users/:id` - Delete user

### System
- `GET /api/health` - Health check
- `POST /api/echo` - Echo endpoint for testing

### Admin
- `GET /admin/dashboard` - Admin dashboard
- `GET /admin/stats` - Admin statistics

## Configuration

### Database (PostgreSQL)
```hocon
db.default.url="jdbc:postgresql://localhost:5432/playdb"
db.default.username="postgres"
db.default.password="password"
```

### CORS Configuration
```hocon
play.filters.cors {
  allowedOrigins = null  # Allow any origin (development)
  allowedHttpMethods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
  allowCredentials = true
}
```

## Docker Support

```bash
# Start PostgreSQL only
docker compose up -d

# Stop PostgreSQL
docker compose down

# View logs
docker compose logs postgres

# Clean up (removes volumes and data)
docker compose down -v
```

## Frontend Integration

This is a **backend-only** project. Frontend applications can consume the REST API at:
- **Local Development:** http://localhost:9000
- **CORS enabled** for cross-origin requests from any frontend

### API Response Format

All API responses follow this standardized format:
```json
{
  "success": true,
  "data": {...},
  "message": "Success message",
  "timestamp": "2025-09-15T08:05:03.140Z"
}
```

## Development

### Testing
```bash
sbt test
```

### Database Migration Commands

```bash
# Check migration status
sbt flywayInfo

# Run pending migrations
sbt flywayMigrate

# Repair failed migrations
sbt flywayRepair

# Clean database (WARNING: Destroys all data!)
sbt flywayClean
```

### Creating New Migrations

1. Create a new migration file in `src/main/resources/db/migration/`:
   ```
   V3__Add_products_table.sql
   ```

2. Add your SQL:
   ```sql
   CREATE TABLE products (
     id BIGSERIAL PRIMARY KEY,
     name VARCHAR(255) NOT NULL,
     price DECIMAL(10,2) NOT NULL
   );
   ```

3. Run migration:
   ```bash
   sbt flywayMigrate
   ```

### Database Management
```bash
# Connect to database directly
docker exec -it scala-play-postgres psql -U postgres -d playdb

# Check table contents
docker exec scala-play-postgres psql -U postgres -d playdb -c "SELECT * FROM users;"

# View migration history
docker exec scala-play-postgres psql -U postgres -d playdb -c "SELECT * FROM flyway_schema_history;"
```

## Troubleshooting

### Common Issues
1. **Port 9000 already in use**: Stop other services or change port in `application.conf`
2. **Database connection failed**: Ensure PostgreSQL container is running with `docker compose ps`
3. **Java version issues**: Use `./run.sh` script instead of `sbt run`

### Logs and Debugging
```bash
# View application logs
tail -f logs/application.log

# View Docker logs
docker compose logs -f postgres

# Check if database is accessible
docker exec scala-play-postgres pg_isready -U postgres
```
