FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache curl bash git && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz" | tar xz -C /opt/ && \
    ln -s /opt/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /app

COPY build.sbt ./
COPY project ./project
RUN sbt update

CMD ["sbt", "~run"]
