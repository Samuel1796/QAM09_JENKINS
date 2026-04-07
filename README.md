# Jenkins CI/CD API Testing

Automated REST API test suite for the [FakeStore API](https://fakestoreapi.com/), integrated with a Jenkins CI/CD pipeline. Tests are written in Java using REST Assured and TestNG, with property-based tests powered by jqwik. The pipeline runs on every GitHub push via webhook, publishes JUnit XML, Surefire HTML, and Allure reports, and sends structured Slack notifications on build completion.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 17 |
| Maven | 3.9+ |
| Docker | 20.10+ |

---

## Running Tests Locally

**Run all tests (unit + property-based):**
```bash
mvn test
```

**Generate HTML report after tests:**
```bash
mvn surefire-report:report
```

Reports are written to:
- JUnit XML: `target/surefire-reports/*.xml`
- Surefire HTML report: `target/site/surefire-report.html`
- Allure results: `target/allure-results/`
- Allure report: `target/allure-report/`

---

## Running Tests with Docker

**Build the image:**
```bash
docker build -t fakestore-api-tests .
```

**Run tests and extract reports:**
```bash
docker run --rm -v "$(pwd)/target:/app/target" fakestore-api-tests
```

Reports will be available in your local `target/` directory after the container exits, including the Allure results folder.

---

## Jenkins Setup

### 1. Start Jenkins

Launch Jenkins using the official LTS Docker image:

```bash
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

Jenkins will be available at `http://localhost:8080`. Retrieve the initial admin password with:

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### 2. Install Required Plugins

After completing the initial setup wizard, install the following plugins via **Manage Jenkins → Plugins → Available plugins**:

| Plugin | Purpose |
|--------|---------|
| Git | Source code checkout |
| Pipeline | Declarative pipeline support |
| HTML Publisher | Publish HTML test reports |
| JUnit | Archive and display JUnit XML results |
| Slack Notification | Send build notifications to Slack |

### 3. Configure Slack Notifications

1. Go to **Manage Jenkins → System**.
2. Scroll to the **Slack** section.
3. Enter your Slack workspace name and add a **Bot Token** credential.
4. Set the default channel (e.g. `#ci-notifications`).
5. Click **Test Connection** to verify.

### 4. Create the Pipeline Job

1. Click **New Item**, enter a name, select **Pipeline**, and click **OK**.
2. Under **Build Triggers**, check **GitHub hook trigger for GITScm polling**.
3. Under **Pipeline**, set **Definition** to `Pipeline script from SCM`.
4. Set **SCM** to `Git` and enter your repository URL.
5. Set the branch to build (e.g. `*/main`).
6. Set **Script Path** to `Jenkinsfile`.
7. Click **Save**.

### 5. Configure the GitHub Webhook

1. In your GitHub repository, go to **Settings → Webhooks → Add webhook**.
2. Set **Payload URL** to `http://<jenkins-host>:8080/github-webhook/`.
3. Set **Content type** to `application/json`.
4. Under **Which events**, select **Just the push event**.
5. Click **Add webhook**.

After saving, every push to the configured branch will automatically trigger the Jenkins pipeline within ~30 seconds.

---

## Pipeline Stages

| Stage | Description |
|-------|-------------|
| Checkout | Clones the repository |
| Build & Install Deps | Downloads Maven dependencies (`mvn dependency:resolve`) |
| Run Tests | Executes the full test suite (`mvn test`) |
| Generate Allure Report | Builds the Allure HTML report under `target/allure-report/` |
| Generate Surefire HTML Report | Builds the Maven test report under `target/site/` |
| Publish Reports | Archives JUnit XML results and publishes the Surefire HTML report |

Build results and reports are accessible from the Jenkins build page. Slack notifications are sent on both success and failure, and include the passed/failed test names plus a test count summary.
