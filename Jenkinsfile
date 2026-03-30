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
                // Continue pipeline even if tests fail so reports always publish
                bat 'mvn test -B || exit 0'
            }
        }

        stage('Publish Reports') {
            steps {
                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
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

            script {
                def buildStatus = currentBuild.currentResult ?: 'SUCCESS'
                def color = buildStatus == 'SUCCESS' ? 'good' : 'danger'

                // Extract failed test names from JUnit XML using powershell
                def failedTests = ''
                try {
                    def output = bat(
                        returnStdout: true,
                        script: '''@echo off
powershell -Command "
$files = Get-ChildItem -Path 'target\\surefire-reports' -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue
$failures = @()
foreach ($f in $files) {
    [xml]$xml = Get-Content $f.FullName
    foreach ($tc in $xml.testsuite.testcase) {
        if ($tc.failure -or $tc.error) {
            $failures += $tc.name
        }
    }
}
if ($failures.Count -gt 0) { $failures -join '|' } else { '' }
"'''
                    ).trim()
                    if (output) {
                        failedTests = '\nFailed tests:\n' + output.split('\\|').collect { "  • ${it}" }.join('\n')
                    }
                } catch (e) {
                    failedTests = ''
                }

                def msg = "${buildStatus}: Job '${env.JOB_NAME}' #${env.BUILD_NUMBER}${failedTests}\n${env.BUILD_URL}"

                catchError(buildResult: buildStatus) {
                    slackSend(color: color, message: msg)
                }
            }
        }
    }
}
