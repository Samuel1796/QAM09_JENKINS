# Project Overview: Jenkins CI/CD for QA API Automation

## 1) Project Purpose
This project gives QA engineers hands-on experience building a CI/CD pipeline with Jenkins for API test automation.

It simulates a practical workflow where Jenkins:
1. pulls the latest test code from GitHub,
2. executes the Maven/TestNG REST Assured suite,
3. generates and publishes reports,
4. sends team notifications after each run.

---

## 2) End-to-End Flow (How Components Are Linked)
1. Developer pushes code to GitHub.
2. GitHub webhook triggers Jenkins (`githubPush()` in `Jenkinsfile`).
3. Jenkins checks out code and resolves dependencies.
4. Jenkins runs API tests (`mvn test`).
5. Jenkins generates reports (Surefire + Allure in `target/`).
6. Jenkins archives artifacts and publishes HTML report.
7. Jenkins sends Slack notification with test summary and failed tests (if any).

---

## 3) Objectives (OBJs) and Traceability

| OBJ ID | Objective | What It Means in This Project | Evidence in Repo |
|---|---|---|---|
| OBJ-1 | Understand how CI/CD supports test automation | Automated pipeline executes tests consistently on each change | `Jenkinsfile`, `README.md` |
| OBJ-2 | Learn how to create Jenkins jobs/pipelines for testing | Declarative Jenkins pipeline with clear CI stages and post actions | `Jenkinsfile` |
| OBJ-3 | Automate code pulls, test execution, and reporting | SCM checkout, dependency resolution, test run, report generation, report publishing | `Jenkinsfile`, `pom.xml` |
| OBJ-4 | Integrate reports and basic alerting in Jenkins | JUnit + HTML + Allure artifact handling, Slack build notification | `Jenkinsfile`, `JENKINS_SETUP.md`, `README.md` |

---

## 4) Task-by-Task Implementation Mapping

### Task 1: A working test suite
- Implemented API tests using REST Assured + TestNG.
- Test classes are organized by domain for QA readability:
  - `src/test/java/org/example/api/auth/AuthApiTest.java`
  - `src/test/java/org/example/api/users/UsersApiTest.java`
  - `src/test/java/org/example/api/products/ProductsApiTest.java`
  - `src/test/java/org/example/api/carts/CartsApiTest.java`
  - shared setup: `src/test/java/org/example/api/base/BaseApiTest.java`
- Project includes required files:
  - `README.md`
  - `Dockerfile`
  - test source files under `src/test/java/...`

### Task 2: Set up Jenkins
- Jenkins runtime approach documented (local/Docker).
- Required plugin setup documented (Git, Pipeline, HTML Publisher, JUnit, Slack).
- Setup instructions are captured in:
  - `JENKINS_SETUP.md`
  - `README.md`

### Task 3: Configure a Jenkins pipeline job
Pipeline stages in `Jenkinsfile` cover:
- `Checkout` (pull code from SCM)
- `Build & Install Deps`
- `Run Tests`
- `Generate Allure Report`
- `Generate Surefire HTML Report`
- `Publish Reports`

Post-build actions include:
- `archiveArtifacts` for report artifacts in `target/`
- `junit` publishing for XML test results
- HTML report publishing

### Task 4: Webhook and notification
- Push trigger configured in pipeline: `githubPush()`.
- Webhook setup steps documented in `JENKINS_SETUP.md`.
- Slack notification integrated in `Jenkinsfile` with:
  - build status,
  - test summary counts,
  - failed test list (only if failures exist).

---

## 5) Metrics and Scoring Alignment

| Metric | Score | Coverage in Project | Evidence |
|---|---:|---|---|
| Test suite scripts | 20 | Comprehensive REST API tests across auth/users/products/carts | `src/test/java/org/example/api/**` |
| Jenkins setup | 25 | Installation, tools, plugin, Slack, webhook setup documentation | `JENKINS_SETUP.md`, `README.md` |
| Jenkins pipeline | 40 | Full CI test pipeline with reports and artifacts | `Jenkinsfile` |
| Notifications | 15 | Slack notification with status and failed tests | `Jenkinsfile` |
| **Total** | **100** | End-to-end QA CI workflow implemented | Repo-wide evidence |

---

## 6) Key Technical Artifacts
- Pipeline definition: `Jenkinsfile`
- Build/test configuration: `pom.xml`
- Containerized test runner: `Dockerfile`
- Jenkins and webhook setup guide: `JENKINS_SETUP.md`
- Project usage and run guide: `README.md`

---

## 7) Expected Output of a CI Run
A successful pipeline run should produce:
- Test execution in Jenkins console,
- JUnit XML under `target/surefire-reports/`,
- Surefire HTML report under `target/site/`,
- Allure results and report under `target/allure-results/` and `target/allure-report/`,
- Slack message with status and failed tests (if any).

