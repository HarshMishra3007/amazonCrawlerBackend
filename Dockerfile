# Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package

# Run — official Playwright Java image includes Chromium + system deps
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV PORT=8080
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

COPY --from=build /app/target/product-intelligence-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
