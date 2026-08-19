# ============================================
# MetroHub Backend - Multi-Stage Dockerfile
# ============================================
# Stage 1: Build with Maven
# Stage 2: Run with JRE Alpine + Tesseract OCR
# ============================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency layer caching
# Dependencies are re-downloaded only when pom.xml changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install Tesseract OCR for document processing
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    && rm -rf /var/cache/apk/*

# Create non-root user (Docker security best practice)
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Create temp directory for Spring multipart (auto-cleaned after requests)
RUN mkdir -p /tmp/metrohub-uploads && \
    chown appuser:appgroup /tmp/metrohub-uploads

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check using Spring Actuator
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/actuator/health || exit 1

# JVM flags optimized for containers
# UseContainerSupport: respect Docker memory limits
# MaxRAMPercentage: use 75% of container memory for heap
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=55.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
