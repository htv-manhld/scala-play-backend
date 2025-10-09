#!/bin/bash
# Sync common-monitoring files to all services

COMMON_DIR="microservices/common-monitoring"
SERVICES=("user-service" "notification-service" "analytics-service" "api-gateway")

echo "🔄 Syncing monitoring files from $COMMON_DIR..."

for service in "${SERVICES[@]}"; do
  SERVICE_DIR="microservices/$service"

  echo "  → Syncing to $service..."

  # Copy controllers
  cp -f "$COMMON_DIR/app/controllers/MetricsController.scala" "$SERVICE_DIR/app/controllers/"

  # Copy modules
  cp -f "$COMMON_DIR/app/modules/PrometheusModule.scala" "$SERVICE_DIR/app/modules/"
  cp -f "$COMMON_DIR/app/modules/ConsulModule.scala" "$SERVICE_DIR/app/modules/"
done

echo "✅ Sync complete!"
echo ""
echo "📝 Monitoring dependencies in build.sbt:"
echo '  "io.prometheus" % "simpleclient" % "0.16.0",'
echo '  "io.prometheus" % "simpleclient_hotspot" % "0.16.0",'
echo '  "io.prometheus" % "simpleclient_servlet" % "0.16.0",'
echo '  "com.orbitz.consul" % "consul-client" % "1.5.3"'
