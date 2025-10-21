# Multistage Docker build for PUPHAX Hungarian Frontend Service

# Stage 1: Build stage
FROM maven:3.9.4-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first (for better caching)
COPY mvnw* ./
COPY .mvn .mvn
COPY pom.xml ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Set environment variables for Hungarian locale and UTF-8
ENV LANG=hu_HU.UTF-8 \
    LANGUAGE=hu_HU:hu \
    LC_ALL=hu_HU.UTF-8 \
    TZ=Europe/Budapest

# Install required packages and Hungarian locale
RUN apk add --no-cache \
    tzdata \
    curl \
    bash \
    musl-locales \
    musl-locales-lang \
    && cp /usr/share/zoneinfo/Europe/Budapest /etc/localtime \
    && echo "Europe/Budapest" > /etc/timezone

# Create application user
RUN addgroup -g 1001 -S puphax && \
    adduser -u 1001 -S puphax -G puphax

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/puphax-rest-api-1.0.0.jar app.jar

# Copy resources directory structure for SOAP client and frontend
COPY --from=builder /app/src/main/resources ./src/main/resources/

# Change ownership to application user
RUN chown -R puphax:puphax /app

# Switch to application user
USER puphax

# Expose port 8080
EXPOSE 8080

# Health check for the application
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/gyogyszerek/egeszseg/gyors || exit 1

# JVM optimization arguments
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -Dfile.encoding=UTF-8 -Duser.timezone=Europe/Budapest"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]