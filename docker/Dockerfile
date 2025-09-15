# Multi-stage build for Scala Play Framework application
FROM eclipse-temurin:21-jdk-alpine AS build

# Install sbt directly
RUN apk add --no-cache curl bash && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz" | tar xz -C /opt/ && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

# Set Java environment variables (matching run.sh)
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH=$JAVA_HOME/bin:$PATH

# Set working directory
WORKDIR /app

# Copy build files first for dependency caching
COPY build.sbt ./
COPY project/ ./project/

# Cache dependencies - this step will be cached unless build.sbt or project/ changes
RUN sbt update

# Copy all source code
COPY . .

# Build the application using clean compile stage (more reliable than just stage)
RUN sbt clean compile stage

# Production stage
FROM eclipse-temurin:21-jre-alpine

# Install curl and bash for health check and runtime
RUN apk add --no-cache curl bash

# Create app user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built application
COPY --from=build /app/target/universal/stage/ ./
COPY --from=build /app/conf/ ./conf/

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to app user
USER appuser

# Expose port
EXPOSE 9000

# Health check - disabled until /health endpoint is implemented
# HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
#   CMD curl -f http://localhost:9000/health || exit 1

# Start the application (using the staged binary)
CMD ["sh", "-c", "./bin/scala-play-backend -Dplay.http.secret.key=${PLAY_HTTP_SECRET_KEY} -Dconfig.file=conf/application.conf"]