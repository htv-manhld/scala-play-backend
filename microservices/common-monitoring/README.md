# Common Monitoring Module

Shared monitoring and service discovery module for all microservices.

## Features

- **Prometheus Metrics**: JVM metrics (memory, GC, threads, classloading)
- **Consul Service Discovery**: Auto-register/deregister with Consul
- **Health Checks**: Automatic health check registration

## Architecture

```
common-monitoring/
├── app/
│   ├── controllers/
│   │   └── MetricsController.scala    # Exposes /metrics endpoint
│   └── modules/
│       ├── PrometheusModule.scala     # Registers Prometheus collectors
│       └── ConsulModule.scala         # Handles Consul registration
└── README.md
```

## Setup for New Services

### 1. Add Dependencies to build.sbt

```scala
lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      guice,
      // ... other dependencies ...

      // Monitoring dependencies
      "io.prometheus" % "simpleclient" % "0.16.0",
      "io.prometheus" % "simpleclient_hotspot" % "0.16.0",
      "io.prometheus" % "simpleclient_servlet" % "0.16.0",
      "com.orbitz.consul" % "consul-client" % "1.5.3"
    )
  )
```

### 2. Copy Monitoring Files

Run the sync script to copy monitoring files to your service:

```bash
./scripts/sync-monitoring.sh
```

This will copy:
- `controllers/MetricsController.scala`
- `modules/PrometheusModule.scala`
- `modules/ConsulModule.scala`

### 3. Configure application.conf

Add the following to `conf/application.conf`:

```hocon
# Enable monitoring modules
play.modules.enabled += "modules.PrometheusModule"
play.modules.enabled += "modules.ConsulModule"

# Consul configuration
consul.host="consul"
consul.host=${?CONSUL_HOST}
consul.port=8500
consul.port=${?CONSUL_PORT}
consul.service.name="your-service-name"        # Change this
consul.service.host="your-service-name"        # Change this
consul.service.port=9000                        # External port for Consul
```

**Important**:
- `consul.service.name` and `consul.service.host` should match your service name
- `consul.service.port` is the **external** port (e.g., 9001, 9002)
- The health check will use `play.server.http.port` (internal port, usually 9000)

### 4. Add Routes

Add to `conf/routes`:

```
# Health check endpoint
GET     /health                     controllers.HealthController.health()

# Metrics for Prometheus
GET     /metrics                    controllers.MetricsController.metrics()
```

### 5. Update docker-compose.yml

Ensure your service has correct port mapping:

```yaml
your-service:
  build:
    context: ./microservices/your-service
    dockerfile: Dockerfile
  ports:
    - "9001:9000"  # External:Internal
  env_file:
    - ./microservices/your-service/.env
  depends_on:
    consul:
      condition: service_started
```

## Testing

### Check Service Health

```bash
# Direct health check
curl http://localhost:9001/health

# Via Consul
curl http://localhost:8500/v1/health/service/your-service-name
```

### Check Metrics

```bash
# View Prometheus metrics
curl http://localhost:9001/metrics
```

### Verify Consul Registration

```bash
# List all registered services
curl http://localhost:8500/v1/catalog/services | jq .

# Check specific service health
curl http://localhost:8500/v1/health/service/your-service-name | jq .
```

### Verify Prometheus Scraping

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# Query metrics
curl 'http://localhost:9090/api/v1/query?query=jvm_threads_current' | jq .
```

## Monitoring Stack

### Infrastructure Services

- **Consul UI**: http://localhost:8500
  - View registered services
  - Check health status
  - Service discovery

- **Prometheus**: http://localhost:9090
  - Metrics collection
  - Query metrics
  - View targets status

- **Grafana**: http://localhost:3000
  - Create dashboards
  - Visualize metrics
  - Default credentials: `admin/admin`

### Service Endpoints

| Service | Health | Metrics | External Port |
|---------|--------|---------|---------------|
| API Gateway | http://localhost:9000/health | http://localhost:9000/metrics | 9000 |
| User Service | http://localhost:9001/health | http://localhost:9001/metrics | 9001 |
| Notification Service | http://localhost:9002/health | http://localhost:9002/metrics | 9002 |
| Analytics Service | http://localhost:9003/health | http://localhost:9003/metrics | 9003 |

## Troubleshooting

### Consul Health Check Failed

**Problem**: Consul shows service as "critical"

**Solution**: Check that:
1. Service is actually running and healthy
2. Health endpoint returns 200 OK
3. Port mapping is correct (internal port 9000)
4. `play.server.http.port` is configured correctly

```bash
# Check from inside container
docker exec <container-name> curl http://localhost:9000/health

# Check Consul health check details
curl http://localhost:8500/v1/health/service/<service-name> | jq '.[].Checks'
```

### Prometheus Not Scraping

**Problem**: Prometheus target shows as "down"

**Solution**: Verify:
1. Service is registered in `monitoring/prometheus.yml`
2. Target uses correct port (internal port 9000)
3. Service is actually running

```yaml
# prometheus.yml should have:
- job_name: "your-service"
  static_configs:
    - targets: ["your-service:9000"]  # Use internal port
```

### Metrics Not Showing

**Problem**: `/metrics` endpoint returns empty or error

**Solution**:
1. Verify `PrometheusModule` is enabled in `application.conf`
2. Check `MetricsController` is present
3. Verify routes file has metrics route

## Metrics Available

### JVM Metrics (from simpleclient_hotspot)

- **Memory**: `jvm_memory_bytes_used`, `jvm_memory_bytes_max`
- **GC**: `jvm_gc_collection_seconds_count`, `jvm_gc_collection_seconds_sum`
- **Threads**: `jvm_threads_current`, `jvm_threads_daemon`, `jvm_threads_peak`
- **Classes**: `jvm_classes_loaded`, `jvm_classes_unloaded`

### Example Prometheus Queries

```promql
# Current threads per service
jvm_threads_current

# Memory usage (heap)
jvm_memory_bytes_used{area="heap"}

# GC time rate
rate(jvm_gc_collection_seconds_sum[5m])

# Service uptime
up
```

## Maintenance

### Update Monitoring Code

When you update files in `common-monitoring/`, run:

```bash
./scripts/sync-monitoring.sh
```

Then restart affected services:

```bash
docker compose restart user-service notification-service analytics-service api-gateway
```

### Update Prometheus Configuration

After modifying `monitoring/prometheus.yml`:

```bash
docker restart microservices-prometheus
```

## Best Practices

1. **Always sync after changes**: Run `sync-monitoring.sh` after updating common monitoring code
2. **Test locally first**: Verify health and metrics endpoints work before deploying
3. **Monitor Consul**: Check that services register/deregister properly
4. **Use meaningful service names**: Service names should match actual service purpose
5. **External vs Internal ports**: Remember external ports (9001-9003) map to internal port 9000
