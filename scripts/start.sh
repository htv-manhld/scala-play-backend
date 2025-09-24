#!/bin/bash

echo "🚀 Starting DDD + Microservices application..."

# Check if all service .env files exist
services=("api-gateway" "user-service" "notification-service" "analytics-service")
missing_env=false

for service in "${services[@]}"; do
    if [ ! -f "microservices/$service/.env" ]; then
        if [ -f "microservices/$service/.env.example" ]; then
            echo "⚠️  Creating .env for $service from .env.example"
            cp "microservices/$service/.env.example" "microservices/$service/.env"
        else
            echo "❌ Missing .env file for $service and no .env.example found"
            missing_env=true
        fi
    fi
done

if [ "$missing_env" = true ]; then
    echo "❌ Please create missing .env files before starting"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Stop any existing containers
echo "🛑 Stopping existing containers..."
docker compose down

# Build and start infrastructure services first
echo "🏗️ Starting infrastructure services..."
docker compose up -d postgres zookeeper kafka redis consul prometheus grafana

# Wait for infrastructure to be ready
echo "⏳ Waiting for infrastructure services to be ready..."
sleep 30

# Check if Kafka is ready
echo "🔍 Checking Kafka status..."
docker compose exec -T kafka kafka-topics --bootstrap-server localhost:9092 --list &> /dev/null
if [ $? -eq 0 ]; then
    echo "✅ Kafka is ready"
else
    echo "⚠️ Kafka might not be fully ready, but continuing..."
fi

# Start microservices
echo "🚀 Starting microservices..."
docker compose up -d

# Wait for services to start
echo "⏳ Waiting for microservices to start..."
sleep 20

# Check service health
echo "🏥 Checking service health..."
services=("api-gateway:9000" "user-service:9001" "notification-service:9002" "analytics-service:9003")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    echo "Checking $name on port $port..."

    max_retries=10
    retry_count=0

    while [ $retry_count -lt $max_retries ]; do
        if curl -f -s "http://localhost:$port/health" > /dev/null; then
            echo "✅ $name is healthy"
            break
        else
            retry_count=$((retry_count + 1))
            echo "⏳ $name not ready yet (attempt $retry_count/$max_retries)"
            sleep 5
        fi
    done

    if [ $retry_count -eq $max_retries ]; then
        echo "❌ $name failed to start properly"
    fi
done

echo ""
echo "🎉 Application startup complete!"
echo ""
echo "📊 Service URLs:"
echo "  • API Gateway:       http://localhost:9000"
echo "  • User Service:      http://localhost:9001"
echo "  • Notification Svc:  http://localhost:9002"
echo "  • Analytics Service: http://localhost:9003"
echo ""
echo "🔧 Infrastructure URLs:"
echo "  • Consul (Service Discovery): http://localhost:8500"
echo "  • Prometheus (Metrics):       http://localhost:9090"
echo "  • Grafana (Monitoring):       http://localhost:3000"
echo "  • PostgreSQL:                 localhost:5432"
echo "  • Kafka:                      localhost:9092"
echo "  • Redis:                      localhost:6379"
echo ""
echo "📋 Quick test commands:"
echo "  curl http://localhost:9000/health"
echo "  curl http://localhost:9000/api/users"
echo ""
echo "📝 View logs with: docker compose logs -f [service-name]"