# Implementation Plan: Jenkins CI/CD API Testing

## Overview

Incremental implementation starting with Maven configuration, then the test suite, then infrastructure files (Dockerfile, Jenkinsfile), and finally repo hygiene. Each step builds on the previous so nothing is left unintegrated.

## Tasks

- [x] 1. Configure Maven pom.xml with required dependencies
  - Update `pom.xml`: set `maven.compiler.source/target` to `17`
  - Add `io.rest-assured:rest-assured:5.4.0` (test scope)
  - Add `org.testng:testng:7.9.0` (test scope)
  - Add `net.jqwik:jqwik:1.8.4` (test scope) with `junit-platform-launcher` bridge
  - Add `maven-surefire-plugin:3.2.5` configured to include `**/*Test.java`, output XML to `target/surefire-reports/`, and enforce a 120-second suite timeout
  - Add `maven-surefire-report-plugin:3.2.5` for HTML report generation
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 2. Implement BaseApiTest and REST Assured shared configuration
  - Create `src/test/java/org/example/api/BaseApiTest.java`
  - Set `RestAssured.baseURI = "https://fakestoreapi.com"` in a `@BeforeSuite` method
  - Define a shared response-time threshold constant (2000 ms)
  - _Requirements: 1.2, 1.6_

- [x] 3. Implement ProductsApiTest
  - Create `src/test/java/org/example/api/ProductsApiTest.java` extending `BaseApiTest`
  - `GET /products` → assert 200, body contains `id`, `title`, `price`, response time < threshold
  - `GET /products/{id}` with valid ID → assert 200, body fields present
  - `GET /products/0` → assert 404, failure message contains `"404"`
  - _Requirements: 1.1, 1.2, 1.5_

  - [ ]* 3.1 Write unit tests for ProductsApiTest edge cases
    - Test invalid product ID returns 404 with descriptive assertion message
    - _Requirements: 1.5_

- [x] 4. Implement CartsApiTest
  - Create `src/test/java/org/example/api/CartsApiTest.java` extending `BaseApiTest`
  - `GET /carts` → assert 200, body contains `id`, `userId`, response time < threshold
  - `GET /carts/{id}` with valid ID → assert 200, body fields present
  - _Requirements: 1.1, 1.2_

- [x] 5. Implement UsersApiTest
  - Create `src/test/java/org/example/api/UsersApiTest.java` extending `BaseApiTest`
  - `GET /users` → assert 200, body contains `id`, `email`, response time < threshold
  - `GET /users/{id}` with valid ID → assert 200, body fields present
  - _Requirements: 1.1, 1.2_

- [x] 6. Implement AuthApiTest
  - Create `src/test/java/org/example/api/AuthApiTest.java` extending `BaseApiTest`
  - `POST /auth/login` with valid credentials → assert 200, body contains `token`
  - `POST /auth/login` with invalid credentials → assert 401, failure message contains `"401"`
  - _Requirements: 1.1, 1.2, 1.5_

- [x] 7. Checkpoint — verify test suite runs end-to-end
  - Ensure all tests pass, ask the user if questions arise.
  - Confirm `target/surefire-reports/*.xml` is generated after `mvn test`
  - _Requirements: 1.3_

- [x] 8. Implement property-based tests
  - Create `src/test/java/org/example/api/PropertyTests.java`
  - Add jqwik `@Provide` generators: `errorStatusCodes` (4xx/5xx), `validEndpoints` (FakeStore paths), `buildOutcomes` (random job names, build numbers, URLs, statuses)
  - Implement `SlackNotifier.formatMessage(BuildOutcome)` helper class used by Property 3

  - [x] 8.1 Implement Property 1 test: report generation invariant
    - `@Property(tries = 100) void reportFilesExistAfterTestRun(...)` — asserts at least one `.xml` in `target/surefire-reports/` and one `.html` in `target/` for any valid test-run scenario
    - Tag: `// Feature: jenkins-cicd-api-testing, Property 1: Test run produces both XML and HTML reports`
    - _Requirements: 1.3, 1.4_

  - [x] 8.2 Implement Property 2 test: error response causes failure with descriptive message
    - `@Property(tries = 100) void errorStatusCodeCausesFailureWithMessage(int statusCode, String endpoint)` — asserts `AssertionError` message contains the actual status code string
    - Tag: `// Feature: jenkins-cicd-api-testing, Property 2: API error responses cause test failure with descriptive message`
    - _Requirements: 1.5_

  - [x] 8.3 Implement Property 3 test: Slack message contains all required fields
    - `@Property(tries = 100) void slackMessageContainsRequiredFields(BuildOutcome outcome)` — asserts message contains status, job name, build number, and build URL
    - Tag: `// Feature: jenkins-cicd-api-testing, Property 3: Slack notification message contains all required fields`
    - _Requirements: 9.1_

- [x] 9. Checkpoint — verify property tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Create Dockerfile at repository root
  - Base image: `maven:3.9-eclipse-temurin-17`
  - `WORKDIR /app`
  - `COPY pom.xml .` then `RUN mvn dependency:go-offline -B` (layer cache)
  - `COPY src/ src/`
  - `VOLUME ["/app/target"]`
  - `CMD ["mvn", "test"]`
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 11. Create Jenkinsfile at repository root
  - Declarative pipeline with `agent any`
  - `tools` block: `maven 'Maven 3.9'`, `jdk 'JDK 17'`
  - `triggers { githubPush() }`
  - Stages in order: `Checkout` (`checkout scm`), `Build & Install Deps` (`mvn dependency:resolve -B`), `Run Tests` (`mvn test -B`), `Publish Reports` (`junit '**/surefire-reports/*.xml'` + `publishHTML`)
  - `post.always`: archive JUnit XML and HTML report
  - `post.success`: `slackSend(color: 'good', message: ...)` wrapped in `catchError(buildResult: 'SUCCESS')`
  - `post.failure`: `slackSend(color: 'danger', message: ...)` wrapped in `catchError(buildResult: 'SUCCESS')`
  - Slack message includes `${env.JOB_NAME}`, `${env.BUILD_NUMBER}`, `${env.BUILD_URL}`, build status
  - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 9.1, 9.2, 9.3, 9.4_

- [x] 12. Update .gitignore
  - Ensure `target/`, `*.class`, `.idea/`, `*.iml`, `.DS_Store` are excluded
  - _Requirements: 4.4_

- [x] 13. Create README.md
  - Document project purpose, prerequisites (Java 17, Maven 3.9, Docker)
  - Local test execution: `mvn test` and `mvn surefire-report:report`
  - Docker execution: `docker build` + `docker run` commands
  - Jenkins setup: `docker run` command for `jenkins/jenkins:lts`, required plugins list, webhook configuration steps, pipeline job creation steps
  - _Requirements: 4.1, 5.1, 5.2, 5.3, 8.1, 8.2, 8.3_

- [x] 14. Final checkpoint — full integration check
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Property tests (tasks 8.1–8.3) use jqwik with 100 iterations each and must be tagged with the property comment format
- `SlackNotifier` helper class (task 8) is a plain Java class — not a Jenkins plugin call — used only for unit-testable message formatting
- Slack `slackSend` calls in the Jenkinsfile must be wrapped in `catchError` so Slack outages do not affect build status (Requirement 9.4)
- The `post.always` block ensures reports are archived even when the test stage fails (Requirement 6.5)
