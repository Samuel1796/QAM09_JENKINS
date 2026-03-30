FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

# Copy pom.xml first and download dependencies (layer cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source files
COPY src/ src/

# Expose target directory as a volume so Jenkins can retrieve reports
VOLUME ["/app/target"]

CMD ["mvn", "test"]
