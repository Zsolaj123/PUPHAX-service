# Optimized Multistage Docker build for PUPHAX REST API Service

# Stage 1: Build stage with dependency caching optimization
FROM maven:3.9.4-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Configure Maven settings for better performance and reliability
RUN mkdir -p /root/.m2 && \
    echo '<settings><localRepository>/root/.m2/repository</localRepository><mirrors><mirror><id>central</id><name>Central Repository</name><url>https://repo1.maven.org/maven2</url><mirrorOf>central</mirrorOf></mirror></mirrors></settings>' > /root/.m2/settings.xml

# Copy Maven wrapper and pom.xml first (for better caching)
COPY mvnw* ./
COPY .mvn .mvn
COPY pom.xml ./

# Make Maven wrapper executable and set appropriate permissions
RUN chmod +x ./mvnw

# Set JAVA_HOME explicitly for Maven wrapper
ENV JAVA_HOME=/opt/java/openjdk

# Download dependencies with increased timeout and retry logic
# This layer will be cached if pom.xml doesn't change
RUN ./mvnw dependency:go-offline -B \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.pool=false \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -Dmaven.wagon.http.ssl.insecure=false \
    -Dmaven.wagon.http.ssl.allowall=false

# Copy WSDL resources first (needed for code generation)
COPY src/main/resources/wsdl ./src/main/resources/wsdl/

# Generate SOAP client classes from WSDL
RUN ./mvnw jaxws:wsimport -B

# Copy source code
COPY src ./src

# Build the application with optimized settings
RUN ./mvnw clean package -DskipTests -B \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.http.pool=false \
    --fail-at-end \
    --batch-mode \
    --no-transfer-progress

# Stage 2: Optimized Runtime stage with security and Hungarian locale support
FROM eclipse-temurin:17-jre-alpine

# Set environment variables for Hungarian locale and UTF-8 with enhanced encoding support
ENV LANG=hu_HU.UTF-8 \
    LANGUAGE=hu_HU:hu \
    LC_ALL=hu_HU.UTF-8 \
    TZ=Europe/Budapest \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"

# Install required packages with security updates and Hungarian locale support
RUN apk add --no-cache --update \
    tzdata \
    curl \
    bash \
    ca-certificates \
    musl-locales \
    musl-locales-lang \
    && cp /usr/share/zoneinfo/Europe/Budapest /etc/localtime \
    && echo "Europe/Budapest" > /etc/timezone \
    && update-ca-certificates

# Create application user with restricted permissions
RUN addgroup -g 1001 -S puphax && \
    adduser -u 1001 -S puphax -G puphax -s /bin/bash

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/puphax-rest-api-1.0.0.jar app.jar

# Copy only necessary resources (optimized)
COPY --from=builder /app/src/main/resources/static ./static/
COPY --from=builder /app/src/main/resources/wsdl ./wsdl/

# Create logs directory
RUN mkdir -p /app/logs

# Change ownership to application user
RUN chown -R puphax:puphax /app

# Switch to application user for security
USER puphax

# Expose port 8081 (matches application.yml server.port)
EXPOSE 8081

# Enhanced health check with proper endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8081/api/v1/gyogyszerek/egeszseg/gyors || exit 1

# Optimized JVM arguments for PUPHAX service with Hungarian character support
ENV JAVA_OPTS="-server \
    -Xms512m \
    -Xmx1024m \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -XX:+OptimizeStringConcat \
    -XX:MaxGCPauseMillis=200 \
    -Dfile.encoding=UTF-8 \
    -Dsun.jnu.encoding=UTF-8 \
    -Duser.timezone=Europe/Budapest \
    -Djava.awt.headless=true \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=production"

# PUPHAX service configuration (override in production with docker-compose or environment)
ENV PUPHAX_USERNAME=PUPHAX \
    PUPHAX_PASSWORD=puphax \
    PUPHAX_ENDPOINT_URL=https://puphax.neak.gov.hu/PUPHAXWS \
    SPRING_PROFILES_ACTIVE=production \
    LOGGING_LEVEL_COM_PUPHAX=INFO

# Run the application with enhanced startup
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]