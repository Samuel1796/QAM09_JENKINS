# Jenkinsfile Explanation

This document explains the logic, Jenkins keywords, and methods used in `Jenkinsfile` for code review.

## 1) Pipeline Purpose

The pipeline automates QA execution after every GitHub push:
- Checks out the latest code.
- Resolves Maven dependencies.
- Runs API tests.
- Generates Allure and Surefire reports.
- Publishes test results in Jenkins.
- Sends a structured Slack notification with test summary and failed test list (only when failures exist).

## 2) Top-Level Jenkins Directives

### `pipeline {}`
Defines a Declarative Pipeline.

### `agent any`
Runs the pipeline on any available Jenkins executor/node.

### `tools {}`
Pins required build tools configured in Jenkins Global Tool Configuration:
- `maven 'Maven 3.9'`
- `jdk 'JDK 17'`

### `triggers { githubPush() }`
Enables webhook-driven builds from GitHub push events.

## 3) Stage-by-Stage Flow

### Stage: `Checkout`
- Method: `checkout scm`
- Function: pulls the exact repository revision/branch configured in the job.

### Stage: `Build & Install Deps`
- Command: `mvn dependency:resolve -B`
- Function: resolves and caches dependencies before test execution.
- `-B` = Maven batch mode (non-interactive, CI-friendly logs).

### Stage: `Run Tests`
- Command: `mvn test -B`
- Wrapped with: `catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE')`
- Function: executes test suite while allowing pipeline continuation for report publishing.

### Stage: `Generate Allure Report`
- Command: `mvn allure:report -B`
- Wrapped with `catchError(...)`.
- Function: generates Allure output under:
  - `target/allure-results/`
  - `target/allure-report/`

### Stage: `Generate Surefire HTML Report`
- Command: `mvn surefire-report:report -B`
- Wrapped with `catchError(...)`.
- Function: generates HTML report in `target/site/surefire-report.html`.

### Stage: `Publish Reports`
- Method: `junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'`
  - Function: publishes JUnit XML into Jenkins test trends/history.
- Method: `publishHTML(...)`
  - Function: exposes Surefire HTML report in Jenkins build UI.

## 4) Why `catchError` Is Used

`catchError` prevents hard pipeline stop when tests/report generation fails. This is important because QA evidence (JUnit XML, HTML reports, Allure output, Slack alerts) should still be produced for analysis.

## 5) Post-Build Block (`post { always { ... } }`)

The `always` block runs for success, unstable, and failure builds.

### Artifact Archiving
Uses `archiveArtifacts(..., allowEmptyArchive: true)` to persist:
- `**/surefire-reports/*.xml`
- `target/allure-results/**`
- `target/allure-report/**`
- `target/site/**`

`allowEmptyArchive: true` avoids extra pipeline failure when some files are missing.

### Slack Summary Preparation
Inside `script { ... }`:
1. Detects build status (`SUCCESS`, `UNSTABLE`, `FAILURE`) and Slack color (`good`, `warning`, `danger`).
2. Runs a PowerShell parser over `target/surefire-reports/TEST-*.xml`.
3. Computes:
   - `TOTAL`
   - `PASSED`
   - `FAILED`
   - `SKIPPED`
   - `FAILED_LIST` (joined with `||`, or `__NONE__` when no failures)
4. Parses key-value output into a Groovy map.
5. Splits `FAILED_LIST` and renders a failed-tests section only when failures exist.
6. Sends formatted message with `slackSend(color: color, message: msg)`.
7. Wraps Slack send in `try/catch` so notification issues do not fail the build.

## 6) Key Jenkins Methods and Keywords in This File

- `checkout scm`: checkout from configured SCM in job.
- `bat '...'`: run Windows shell command on Jenkins agent.
- `powershell(...)`: execute PowerShell and capture output.
- `catchError(...)`: continue pipeline while marking stage/build result.
- `junit(...)`: publish test results in Jenkins.
- `publishHTML(...)`: publish HTML report artifact link.
- `archiveArtifacts(...)`: store generated files in build record.
- `post { always { ... } }`: guaranteed finalization block.
- `slackSend(...)`: send build notification to Slack channel.

## 7) Inputs/Dependencies Required for This Jenkinsfile

- Jenkins tools configured exactly as:
  - `JDK 17`
  - `Maven 3.9`
- Plugins expected:
  - Git
  - Pipeline
  - JUnit
  - HTML Publisher
  - Slack Notification
- Maven project/report outputs (from `pom.xml`):
  - Surefire XML: `target/surefire-reports/`
  - Surefire HTML: `target/site/`
  - Allure results/report: `target/allure-results/`, `target/allure-report/`

## 8) Reviewer Notes

- The pipeline is designed for observability: tests can fail without losing reporting and notification.
- Slack notifications are resilient (`try/catch`) and include failed test names only when failures exist.
- Archiving and publishing are permissive (`allowEmpty...`) to reduce secondary failures in post-processing.

