# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Cache dependencies separately from source changes.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
COPY --from=build /build/target/supportiq-*.jar app.jar
RUN chown -R spring:spring /app
USER spring

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseZGC"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
