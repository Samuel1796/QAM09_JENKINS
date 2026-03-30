pipeline {
    agent any

    tools {
        maven 'Maven 3.9'
        jdk 'JDK 17'
    }

    triggers {
        githubPush()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Install Deps') {
            steps {
                bat 'mvn dependency:resolve -B'
            }
        }

        stage('Run Tests') {
            steps {
                bat 'mvn test -B'
            }
        }

        stage('Publish Reports') {
            steps {
                junit '**/surefire-reports/*.xml'
                publishHTML(target: [
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'target/surefire-reports',
                    reportFiles          : '*.html',
                    reportName           : 'Surefire HTML Report'
                ])
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: '**/surefire-reports/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/**/*.html',          allowEmptyArchive: true
        }

        success {
            catchError(buildResult: 'SUCCESS') {
                slackSend(
                    color  : 'good',
                    message: "SUCCESS: Job '${env.JOB_NAME}' #${env.BUILD_NUMBER} completed successfully. ${env.BUILD_URL}"
                )
            }
        }

        failure {
            catchError(buildResult: 'SUCCESS') {
                slackSend(
                    color  : 'danger',
                    message: "FAILURE: Job '${env.JOB_NAME}' #${env.BUILD_NUMBER} failed. ${env.BUILD_URL}"
                )
            }
        }
    }
}
