# Requirements Document

## Introduction

This feature covers the end-to-end setup of a Jenkins CI/CD pipeline for automated API testing. The project uses a Maven-based Java test suite built with REST Assured, targeting the public FakeStore REST API (https://fakestoreapi.com/). The pipeline is triggered by GitHub webhook pushes, executes tests inside a Jenkins instance running via Docker, and publishes HTML and JUnit test reports. Slack notifications are sent on build completion.

## Glossary

- **Test_Suite**: The Maven/Java project containing REST Assured test classes under `src/test/java`
- **FakeStore_API**: The public REST API at https://fakestoreapi.com/ used as the system under test
- **Jenkins**: The CI/CD automation server running via the `jenkins/jenkins:lts` Docker image
- **Pipeline_Job**: A Jenkins job defined by a Jenkinsfile that orchestrates the CI/CD stages
- **Jenkinsfile**: A declarative pipeline script committed to the repository root
- **GitHub_Repo**: The remote Git repository hosting the project source code and Jenkinsfile
- **Webhook**: A GitHub repository webhook that sends an HTTP POST to Jenkins on push events
- **Report_Publisher**: The Jenkins HTML Publisher and JUnit plugins responsible for archiving test output
- **Notifier**: The Jenkins plugin or pipeline step responsible for sending Slack notifications
- **Dockerfile**: A file at the repository root used to build a container image that can run the test suite

---

## Requirements

### Requirement 1: REST Assured Test Suite

**User Story:** As a QA engineer, I want a REST Assured test suite targeting FakeStore API, so that I can validate API behaviour automatically on every code push.

#### Acceptance Criteria

1. THE Test_Suite SHALL include at least one test class per FakeStore_API resource group (products, carts, users, auth).
2. WHEN a test method executes, THE Test_Suite SHALL assert HTTP status codes, response body fields, and response time against defined thresholds.
3. THE Test_Suite SHALL produce a JUnit-compatible XML report under `target/surefire-reports/` after each Maven test run.
4. THE Test_Suite SHALL produce an HTML report (via Maven Surefire or ExtentReports) under `target/` after each Maven test run.
5. IF the FakeStore_API returns an HTTP 4xx or 5xx status code for a valid request, THEN THE Test_Suite SHALL fail the corresponding test with a descriptive assertion message.
6. THE Test_Suite SHALL complete all tests within 120 seconds on a standard CI runner.

---

### Requirement 2: Maven Project Configuration

**User Story:** As a QA engineer, I want the Maven `pom.xml` configured with all required dependencies, so that the project builds and runs tests with a single `mvn test` command.

#### Acceptance Criteria

1. THE pom.xml SHALL declare dependencies for REST Assured, TestNG or JUnit 5, and the Maven Surefire Plugin.
2. THE pom.xml SHALL set the Java compiler source and target to a version compatible with the declared dependencies (Java 11 or higher).
3. WHEN `mvn test` is executed, THE pom.xml SHALL trigger test discovery and execution without additional configuration flags.
4. IF a declared dependency is unavailable from Maven Central, THEN THE pom.xml SHALL reference an alternative repository that resolves the dependency.

---

### Requirement 3: Dockerfile for Test Execution

**User Story:** As a QA engineer, I want a Dockerfile at the repository root, so that the test suite can run in a reproducible containerised environment.

#### Acceptance Criteria

1. THE Dockerfile SHALL use a base image that includes a compatible JDK and Maven installation.
2. WHEN the Docker image is built, THE Dockerfile SHALL copy the project source and `pom.xml` into the image.
3. WHEN the Docker container is run, THE Dockerfile SHALL execute `mvn test` as the default command.
4. THE Dockerfile SHALL expose the `target/surefire-reports/` directory as a volume or copy artefacts to a predictable output path so Jenkins can retrieve them.

---

### Requirement 4: GitHub Repository Structure

**User Story:** As a QA engineer, I want the repository to contain all required files, so that Jenkins can clone and build the project without manual setup.

#### Acceptance Criteria

1. THE GitHub_Repo SHALL contain a `README.md` describing project purpose, prerequisites, and instructions to run tests locally and via Jenkins.
2. THE GitHub_Repo SHALL contain the `Jenkinsfile` at the repository root.
3. THE GitHub_Repo SHALL contain the `Dockerfile` at the repository root.
4. THE GitHub_Repo SHALL contain a `.gitignore` that excludes `target/`, IDE configuration folders, and compiled class files.

---

### Requirement 5: Jenkins Instance Setup

**User Story:** As a QA engineer, I want Jenkins running via Docker, so that I have a reproducible CI server without a manual host installation.

#### Acceptance Criteria

1. THE Jenkins SHALL be launchable with a single `docker run` or `docker-compose up` command using the `jenkins/jenkins:lts` image.
2. WHEN Jenkins starts for the first time, THE Jenkins SHALL expose its web UI on port 8080 of the host machine.
3. THE Jenkins SHALL have the following plugins installed: Git, Pipeline, HTML Publisher, JUnit, Slack Notification.
4. WHEN a plugin installation fails, THE Jenkins SHALL log the failure and continue starting with the remaining plugins.

---

### Requirement 6: Jenkins Pipeline Job

**User Story:** As a QA engineer, I want a Jenkins Pipeline Job driven by a Jenkinsfile, so that the CI/CD stages are version-controlled and reproducible.

#### Acceptance Criteria

1. THE Pipeline_Job SHALL define the following stages in order: Checkout, Build & Install Dependencies, Run Tests, Publish Reports.
2. WHEN the Checkout stage executes, THE Pipeline_Job SHALL clone the GitHub_Repo branch specified in the job configuration.
3. WHEN the Build & Install Dependencies stage executes, THE Pipeline_Job SHALL run `mvn dependency:resolve` or equivalent to download all declared dependencies.
4. WHEN the Run Tests stage executes, THE Pipeline_Job SHALL run `mvn test` and capture the exit code.
5. IF the Run Tests stage exits with a non-zero code, THEN THE Pipeline_Job SHALL mark the build as FAILED and proceed to the Publish Reports and Notify stages.
6. WHEN the Publish Reports stage executes, THE Report_Publisher SHALL archive JUnit XML results and publish the HTML report so they are accessible from the Jenkins build page.

---

### Requirement 7: Jenkinsfile Definition

**User Story:** As a QA engineer, I want a declarative Jenkinsfile committed to the repo, so that the pipeline is self-documenting and easy to modify.

#### Acceptance Criteria

1. THE Jenkinsfile SHALL use declarative pipeline syntax with an `agent` directive specifying the execution environment.
2. THE Jenkinsfile SHALL define a `tools` or `environment` block that sets the Maven and Java versions to use.
3. THE Jenkinsfile SHALL include a `post` block with `always`, `success`, and `failure` conditions.
4. WHEN the `post.always` block executes, THE Jenkinsfile SHALL invoke the Report_Publisher to archive test results regardless of build outcome.
5. WHEN the `post.failure` block executes, THE Jenkinsfile SHALL invoke the Notifier to send a failure notification.
6. WHEN the `post.success` block executes, THE Jenkinsfile SHALL invoke the Notifier to send a success notification.

---

### Requirement 8: GitHub Webhook Trigger

**User Story:** As a QA engineer, I want a GitHub webhook to trigger the Jenkins pipeline on every push, so that tests run automatically without manual intervention.

#### Acceptance Criteria

1. THE Webhook SHALL be configured in the GitHub_Repo settings to send a push event payload to the Jenkins URL at path `/github-webhook/`.
2. WHEN a developer pushes a commit to the configured branch, THE Webhook SHALL trigger the Pipeline_Job within 30 seconds of the push event.
3. THE Pipeline_Job SHALL be configured with "GitHub hook trigger for GITScm polling" enabled.
4. IF the Webhook delivery fails (non-2xx response from Jenkins), THEN THE GitHub_Repo SHALL record the failed delivery and allow manual re-delivery.

---

### Requirement 9: Notifications

**User Story:** As a QA engineer, I want Slack notifications on build completion, so that the team is informed of test results without checking Jenkins manually.

#### Acceptance Criteria

1. THE Notifier SHALL send a message to the designated Slack channel containing build status, job name, build number, and a link to the build page.
2. WHEN a build transitions from SUCCESS to FAILURE, THE Notifier SHALL send a notification regardless of the previously configured notification filter.
3. WHEN a build transitions from FAILURE to SUCCESS, THE Notifier SHALL send a recovery notification.
4. IF the Notifier cannot reach the Slack API, THEN THE Jenkinsfile SHALL log the delivery failure without causing the build to fail.

---

### Requirement 10: Test Report Archiving and Publishing

**User Story:** As a QA engineer, I want test reports archived and published in Jenkins, so that I can review pass/fail trends and failure details from the Jenkins UI.

#### Acceptance Criteria

1. THE Report_Publisher SHALL archive JUnit XML files matching `**/surefire-reports/*.xml` after every build.
2. THE Report_Publisher SHALL publish the HTML report directory so it is viewable from the Jenkins build page.
3. WHEN multiple builds exist, THE Jenkins SHALL retain test result trend data across builds and display a trend graph on the job page.
4. THE Report_Publisher SHALL make archived reports available for download from the Jenkins build artefacts section.
