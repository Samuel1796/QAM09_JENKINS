FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

# Copy the build descriptor first so Maven dependencies can be cached in their own image layer.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the application and test sources after the dependency layer is stable.
COPY src/ src/

# Keep `target/` outside the container filesystem so Jenkins can collect Surefire and Allure outputs.
VOLUME ["/app/target"]

# The default container action mirrors the Jenkins test step: run the full Maven test suite.
CMD ["mvn", "test"]
