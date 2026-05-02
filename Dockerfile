# ---------- BUILD STAGE ----------
FROM gradle:8-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle dependencies --no-daemon
RUN gradle clean build bootJar --no-daemon

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy only final jar
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar
# Expose application port
EXPOSE 8080
# Create data directory for persistence (needed when working with H2)
# RUN mkdir -p /data
ENTRYPOINT ["java", "-jar", "/app/app.jar"]