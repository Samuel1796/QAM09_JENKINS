# Jenkins CI/CD Setup Guide

Step-by-step instructions to connect this project to a local Jenkins instance via GitHub webhook, using ngrok to expose Jenkins to the internet.

---

## Prerequisites

Make sure you have the following installed before starting:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [ngrok](https://ngrok.com/download) (free account required)
- [Git](https://git-scm.com/)
- A [GitHub](https://github.com) account
- A [Slack](https://slack.com) workspace where you can create apps

---

## Step 1: Push the Project to GitHub

1. Create a new repository on GitHub (e.g. `QAM09_JENKINS`).
2. From your project root, run:

```bash
git init
git add .
git commit -m "initial commit"
git branch -M main
git remote add origin https://github.com/<your-username>/QAM09_JENKINS.git
git push -u origin main
```

---

## Step 2: Start Jenkins via Docker

Run Jenkins using the official LTS image:

```bash
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  jenkins/jenkins:lts
```

Wait about 30 seconds, then open `http://localhost:8080` in your browser.

Retrieve the initial admin password:

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Paste it into the browser, then:
- Click **Install suggested plugins** and wait for it to finish.
- Create your admin user when prompted.
- Leave the Jenkins URL as `http://localhost:8080/` for now — you will update it after setting up ngrok.

---

## Step 3: Install Required Plugins

1. Go to **Manage Jenkins → Plugins → Available plugins**.
2. Search for and install each of the following (tick the checkbox, then click **Install**):

| Plugin | Purpose |
|--------|---------|
| Git | Checkout code from GitHub |
| Pipeline | Declarative pipeline support |
| GitHub Integration | Enables webhook trigger |
| HTML Publisher | Publish HTML test reports |
| JUnit | Archive JUnit XML results |
| Slack Notification | Send build notifications to Slack |

3. Tick **Restart Jenkins when installation is complete** at the bottom.

---

## Step 4: Configure JDK and Maven in Jenkins

Jenkins needs to know where Java and Maven are.

1. Go to **Manage Jenkins → Tools**.

### JDK
2. Scroll to **JDK installations** → click **Add JDK**.
3. Uncheck **Install automatically** if you want to use the JDK inside the container, or leave it checked and select **Install from adoptium.net**.
4. Set **Name** to exactly: `JDK 17`
5. If installing automatically, choose version **17** from the dropdown.

### Maven
6. Scroll to **Maven installations** → click **Add Maven**.
7. Leave **Install automatically** checked.
8. Set **Name** to exactly: `Maven 3.9`
9. Choose the latest **3.9.x** version from the dropdown.

10. Click **Save**.

> The names `JDK 17` and `Maven 3.9` must match exactly what is in the `Jenkinsfile` `tools` block.

---

## Step 5: Expose Jenkins with ngrok

GitHub needs a public URL to send webhook payloads to your local Jenkins.

1. Sign up at [ngrok.com](https://ngrok.com) if you haven't already.
2. Authenticate ngrok (one-time setup):

```bash
ngrok config add-authtoken <your-ngrok-token>
```

3. Start a tunnel to Jenkins:

```bash
ngrok http 8080
```

4. Copy the **Forwarding** URL from the ngrok output. It looks like:

```
https://a1b2-203-0-113-42.ngrok-free.app
```

Keep this terminal open — the tunnel must stay running.

### Update Jenkins URL

5. Go to **Manage Jenkins → System**.
6. Find **Jenkins URL** under the **Jenkins Location** section.
7. Replace `http://localhost:8080/` with your ngrok URL (include the trailing slash):

```
https://a1b2-203-0-113-42.ngrok-free.app/
```

8. Click **Save**.

---

## Step 6: Configure Slack Notifications

### Create a Slack App

1. Go to [api.slack.com/apps](https://api.slack.com/apps) → **Create New App → From scratch**.
2. Name it (e.g. `Jenkins CI`) and select your workspace.
3. Go to **OAuth & Permissions** → scroll to **Scopes → Bot Token Scopes**.
4. Add the scope: `chat:write`
5. Click **Install to Workspace** → **Allow**.
6. Copy the **Bot User OAuth Token** (starts with `xoxb-`).
7. Invite the bot to your target channel in Slack:

```
/invite @Jenkins CI
```

### Add Slack Credentials to Jenkins

8. Go to **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**.
9. Set:
   - **Kind**: Secret text
   - **Secret**: paste your `xoxb-...` token
   - **ID**: `slack-bot-token`
   - **Description**: Slack Bot Token

### Configure the Slack Plugin

10. Go to **Manage Jenkins → System** → scroll to the **Slack** section.
11. Set:
    - **Workspace**: your Slack workspace name (e.g. `myteam`)
    - **Credential**: select `slack-bot-token`
    - **Default channel**: `#your-channel-name`
12. Click **Test Connection** — you should see a green success message.
13. Click **Save**.

---

## Step 7: Create the Jenkins Pipeline Job

1. From the Jenkins dashboard, click **New Item**.
2. Enter a name (e.g. `fakestore-api-tests`), select **Pipeline**, click **OK**.
3. Under **General**, tick **GitHub project** and enter your repo URL:

```
https://github.com/<your-username>/QAM09_JENKINS/
```

4. Under **Build Triggers**, tick **GitHub hook trigger for GITScm polling**.

5. Under **Pipeline**:
   - **Definition**: `Pipeline script from SCM`
   - **SCM**: `Git`
   - **Repository URL**: `https://github.com/<your-username>/QAM09_JENKINS.git`
   - **Branch Specifier**: `*/main`
   - **Script Path**: `Jenkinsfile`

6. Click **Save**.

---

## Step 8: Add the GitHub Webhook

1. Go to your GitHub repository → **Settings → Webhooks → Add webhook**.
2. Set:
   - **Payload URL**: `https://<your-ngrok-url>/github-webhook/`
   - **Content type**: `application/json`
   - **Which events**: select **Just the push event**
3. Click **Add webhook**.

GitHub will send a ping — you should see a green tick next to the webhook confirming delivery.

---

## Step 9: Trigger Your First Build

1. Make a small change to any file (e.g. add a blank line to `README.md`).
2. Commit and push:

```bash
git add .
git commit -m "trigger first jenkins build"
git push
```

3. Go to your Jenkins job — within ~30 seconds you should see a new build start automatically.
4. Click the build number to watch the console output.

---

## Step 10: View Test Reports

After the build completes:

- **JUnit trend graph**: visible on the job's main page.
- **Surefire HTML report**: click **Surefire HTML Report** in the left sidebar of the build.
- **Allure report**: the `target/allure-results/` folder is archived. To view it locally after pulling the workspace:

```bash
mvn allure:serve
```

---

## Troubleshooting

**Webhook shows a red X on GitHub**
- Check that ngrok is still running and the URL hasn't changed (free ngrok URLs reset on restart).
- Update the Jenkins URL and GitHub webhook payload URL with the new ngrok URL.

**Build doesn't trigger on push**
- Confirm **GitHub hook trigger for GITScm polling** is ticked on the job.
- Go to **Manage Jenkins → System Log** and check for webhook delivery errors.

**`mvn test` fails with tool not found**
- Verify the JDK and Maven names in **Manage Jenkins → Tools** match exactly: `JDK 17` and `Maven 3.9`.

**Slack notification not sending**
- Re-run **Test Connection** in the Slack plugin config.
- Confirm the bot is invited to the channel (`/invite @<bot-name>`).

**ngrok session expired**
- Restart ngrok: `ngrok http 8080`
- Update the Jenkins URL and GitHub webhook with the new forwarding URL.
