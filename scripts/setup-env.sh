#!/bin/bash

echo "🔧 Setting up environment files for all microservices..."

services=("api-gateway" "user-service" "notification-service" "analytics-service")

for service in "${services[@]}"; do
    env_file="microservices/$service/.env"
    example_file="microservices/$service/.env.example"

    if [ -f "$example_file" ]; then
        if [ -f "$env_file" ]; then
            echo "⚠️  $service/.env already exists, skipping..."
        else
            echo "✅ Creating $service/.env from .env.example"
            cp "$example_file" "$env_file"
        fi
    else
        echo "❌ Missing $example_file"
    fi
done

echo ""
echo "🎯 Environment setup complete!"
echo ""
echo "📝 Next steps:"
echo "1. Edit each service's .env file with your actual credentials:"
for service in "${services[@]}"; do
    echo "   - microservices/$service/.env"
done
echo ""
echo "2. Pay special attention to:"
echo "   - notification-service/.env (SMTP credentials)"
echo "   - All services (APPLICATION_SECRET should be unique)"
echo "   - Database passwords if needed"
echo ""
echo "3. Start the application:"
echo "   ./scripts/start.sh"