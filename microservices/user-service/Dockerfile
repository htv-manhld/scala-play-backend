FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache curl bash git postgresql-client && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz" | tar xz -C /opt/ && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /app

COPY build.sbt ./
COPY project ./project
RUN sbt update

# Copy migration scripts
COPY scripts/migrate.sh /usr/local/bin/migrate
RUN chmod +x /usr/local/bin/migrate

CMD ["sbt", "~run"]
