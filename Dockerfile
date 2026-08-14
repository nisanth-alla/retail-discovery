# ── Stage 1: Build frontend ──────────────────────────────────────────────────
FROM node:24-slim AS frontend-build

WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build
# Output lands in ../src/main/resources/static/home (relative to project root)
# We copy the dist output explicitly in the Maven stage

# ── Stage 2: Build backend (Maven) ───────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS backend-build

WORKDIR /app

# Cache dependencies first
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source
COPY src/ ./src/

# Copy built frontend assets into the expected location. Vite's configured
# output directory is relative to the repository root, not frontend/.
COPY --from=frontend-build /app/src/main/resources/static/home ./src/main/resources/static/home

# Copy ML model files
COPY *.onnx ./
COPY *.pt   ./

# Build, skipping tests
RUN mvn package -DskipTests -B

# ── Stage 3: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install native libraries required by OpenCV and DJL/PyTorch
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgomp1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    libgl1 \
    && rm -rf /var/lib/apt/lists/*

# Copy the fat JAR
COPY --from=backend-build /app/target/*.jar app.jar

# Copy ML model files
COPY --from=backend-build /app/*.onnx ./
COPY --from=backend-build /app/*.pt   ./

# Copy datastore (product images) and initialize the optional vector-store path.
COPY src/main/resources/static/datastore ./src/main/resources/static/datastore
RUN mkdir -p ./embeddings

# Expose the Spring Boot port
EXPOSE 8080

# API keys are read by Spring from the runtime environment.
RUN useradd --system --create-home appuser \
    && chown -R appuser:appuser /app
USER appuser

ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
