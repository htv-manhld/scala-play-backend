FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache curl bash git postgresql-client && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz" | tar xz -C /opt/ && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /workspace

# Copy root build files
COPY build.sbt ./
COPY project ./project

# Copy all services for multi-module build
COPY common-monitoring ./common-monitoring
COPY user-service ./user-service
COPY notification-service ./notification-service
COPY analytics-service ./analytics-service
COPY api-gateway ./api-gateway

# Download dependencies
RUN sbt update

# Copy migration scripts
COPY user-service/scripts/migrate.sh /usr/local/bin/migrate
RUN chmod +x /usr/local/bin/migrate

WORKDIR /workspace/user-service

CMD ["sbt", "project userService", "~run"]
