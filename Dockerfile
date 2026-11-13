
# ================================
# Stage 1: Build the application
# ================================

FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy backend source code
COPY src ./src

# Build Spring Boot application
RUN mvn clean package -DskipTests -B


# ================================
# Stage 2: Run the application
# ================================

FROM eclipse-temurin:21-jre

WORKDIR /app

# Create a non-root application user
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup appuser

# Copy the compiled Spring Boot JAR
COPY --from=build /app/target/codepilot-ai-0.0.1-SNAPSHOT.jar app.jar

# Run as a non-root user
USER appuser

# Production profile
ENV SPRING_PROFILES_ACTIVE=prod

# Default application port
ENV PORT=8080

# Limit Java memory for free cloud hosting
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:+UseSerialGC"

EXPOSE 8080

# Start CodePilot AI
ENTRYPOINT ["java", "-jar", "app.jar"]
