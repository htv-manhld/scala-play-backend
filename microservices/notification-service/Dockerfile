# Multi-stage build for Notification Service
FROM eclipse-temurin:21-jdk-alpine as build

# Install dependencies
RUN apk add --no-cache curl bash && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz" | tar xz -C /opt/ && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

# Set working directory
WORKDIR /app

# Copy build files first (for better caching)
COPY build.sbt ./
COPY project/ ./project/

# Download dependencies
RUN sbt update

# Copy source code
COPY . .

# Build the application
RUN sbt clean compile stage

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Install runtime dependencies
RUN apk add --no-cache curl bash

# Create application user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy built application from build stage
COPY --from=build /app/target/universal/stage/ ./
COPY --from=build /app/conf/ ./conf/

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 9002

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:9002/health || exit 1

# Start application
CMD ["./bin/notification-service", "-Dplay.server.http.address=0.0.0.0"]