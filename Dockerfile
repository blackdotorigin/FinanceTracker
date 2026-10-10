FROM maven:3.9-eclipse-temurin-25-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .
RUN mvn -B -ntp dependency:go-offline

COPY src/ src/
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
COPY --from=build --chown=app:app /workspace/target/Tracker-*.jar /app/app.jar

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-Xms64m", "-Xmx192m", "-XX:MaxMetaspaceSize=96m", "-XX:ReservedCodeCacheSize=32m", "-XX:MaxDirectMemorySize=16m", "-Xss512k", "-XX:ActiveProcessorCount=2", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
