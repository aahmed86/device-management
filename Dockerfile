# ---------- BUILD STAGE ----------
FROM gradle:8-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Cache dependencies separately to avoid re-downloading on every code change
RUN gradle dependencies --no-daemon
RUN gradle clean build bootJar -x test --no-daemon

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# Security: run as non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
# Copy only final jar
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
# Set ownership
RUN chown appuser:appgroup app.jar
USER appuser
# Expose application port
EXPOSE 8080
# Health check so Docker/Compose knows when the app is ready
# Requires spring-boot-starter-actuator on the classpath
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
# Create data directory for persistence (needed when working with H2)
# RUN mkdir -p /data
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]