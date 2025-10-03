#!/bin/bash
#
# OpenAPI Spec Merge Script
# Aggregates OpenAPI specs from all microservices into API Gateway
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🔧 Starting OpenAPI aggregation process..."

# Define service paths
USER_SERVICE="$PROJECT_ROOT/microservices/user-service"
NOTIFICATION_SERVICE="$PROJECT_ROOT/microservices/notification-service"
ANALYTICS_SERVICE="$PROJECT_ROOT/microservices/analytics-service"
API_GATEWAY="$PROJECT_ROOT/microservices/api-gateway"

# Clean up previous build artifacts
echo "🧹 Cleaning up old artifacts..."
rm -rf "$API_GATEWAY/openapi/fragments"
mkdir -p "$API_GATEWAY/openapi/fragments"

# Bundle each service's OpenAPI spec
SERVICES_FOUND=0

if [ -f "$USER_SERVICE/openapi/openapi.yaml" ]; then
  echo "📦 Bundling User Service spec..."
  redocly bundle "$USER_SERVICE/openapi/openapi.yaml" \
    -o "$API_GATEWAY/openapi/fragments/user-service.yaml"
  echo "✅ User Service bundled"
  SERVICES_FOUND=$((SERVICES_FOUND + 1))
fi

if [ -f "$NOTIFICATION_SERVICE/openapi/openapi.yaml" ]; then
  echo "📦 Bundling Notification Service spec..."
  redocly bundle "$NOTIFICATION_SERVICE/openapi/openapi.yaml" \
    -o "$API_GATEWAY/openapi/fragments/notification-service.yaml"
  echo "✅ Notification Service bundled"
  SERVICES_FOUND=$((SERVICES_FOUND + 1))
fi

if [ -f "$ANALYTICS_SERVICE/openapi/openapi.yaml" ]; then
  echo "📦 Bundling Analytics Service spec..."
  redocly bundle "$ANALYTICS_SERVICE/openapi/openapi.yaml" \
    -o "$API_GATEWAY/openapi/fragments/analytics-service.yaml"
  echo "✅ Analytics Service bundled"
  SERVICES_FOUND=$((SERVICES_FOUND + 1))
fi

if [ $SERVICES_FOUND -eq 0 ]; then
  echo "⚠️  No service specs found!"
  exit 1
fi

# Create aggregated spec
echo "🔀 Aggregating all service specs..."

# Start with API Gateway base
cat > "$API_GATEWAY/openapi/openapi.yaml" << 'EOF'
openapi: 3.1.0
info:
  title: API Gateway - All Services
  description: Central API Gateway aggregating all microservices
  version: 1.0.0
  contact:
    name: API Support
    email: support@example.com

servers:
  - url: http://localhost:9000
    description: Local development API Gateway

EOF

# Merge all service specs
# Skip openapi, info, servers sections (first ~13 lines) from each service
for fragment in "$API_GATEWAY/openapi/fragments"/*.yaml; do
  if [ -f "$fragment" ]; then
    echo "  - Merging $(basename "$fragment")"
    # Append tags, paths, components from each service
    tail -n +14 "$fragment" >> "$API_GATEWAY/openapi/openapi.yaml"
    echo "" >> "$API_GATEWAY/openapi/openapi.yaml"
  fi
done

echo "✅ Aggregated spec created at: $API_GATEWAY/openapi/openapi.yaml"
echo ""

# Bundle the aggregated spec into a single file
echo "📦 Bundling aggregated spec..."
if command -v swagger-cli &> /dev/null; then
  swagger-cli bundle "$API_GATEWAY/openapi/openapi.yaml" \
    -o "$API_GATEWAY/openapi/openapi-bundled.yaml" \
    -t yaml
  echo "✅ Bundled spec created at: $API_GATEWAY/openapi/openapi-bundled.yaml"
else
  echo "⚠️  swagger-cli not found, skipping bundle step"
  echo "   Install with: npm install -g @apidevtools/swagger-cli"
fi

echo ""
echo "📊 Summary:"
[ -f "$API_GATEWAY/openapi/fragments/user-service.yaml" ] && echo "  - User Service: ✅" || echo "  - User Service: ❌"
[ -f "$API_GATEWAY/openapi/fragments/notification-service.yaml" ] && echo "  - Notification Service: ✅" || echo "  - Notification Service: ❌"
[ -f "$API_GATEWAY/openapi/fragments/analytics-service.yaml" ] && echo "  - Analytics Service: ✅" || echo "  - Analytics Service: ❌"
echo ""
echo "🎉 OpenAPI aggregation and bundling completed!"
echo ""
echo "Next steps:"
echo "  1. View spec: npx @redocly/cli preview-docs $API_GATEWAY/openapi/openapi-bundled.yaml"
echo "  2. Upload to Swagger Editor: $API_GATEWAY/openapi/openapi-bundled.yaml"
echo "  3. Generate FE client: cd ../svelte-frontend && bash scripts/generate-api-client.sh"
