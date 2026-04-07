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
                // Pull the current revision so every build runs against the exact commit that triggered Jenkins.
                checkout scm
            }
        }

        stage('Build & Install Deps') {
            steps {
                // Resolve Maven dependencies up front to warm the cache before the test run begins.
                bat 'mvn dependency:resolve -B'
            }
        }

        stage('Run Tests') {
            steps {
                // Keep the pipeline moving if tests fail so we can still archive reports and notify Slack.
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    bat 'mvn test -B'
                }
            }
        }

        stage('Generate Allure Report') {
            steps {
                // The Maven plugin writes Allure results and HTML output under target/allure-results and target/allure-report.
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    bat 'mvn allure:report -B'
                }
            }
        }

        stage('Generate Surefire HTML Report') {
            steps {
                // Build the standard Maven test report so the HTML publisher can expose it from target/site.
                catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                    bat 'mvn surefire-report:report -B'
                }
            }
        }

        stage('Publish Reports') {
            steps {
                // Jenkins uses the JUnit XML files for pass/fail trends and test history.
                junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'

                // Publish the Maven Surefire HTML summary from the standard target/site location.
                publishHTML(target: [
                    allowMissing         : true,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'target/site',
                    reportFiles          : 'surefire-report.html',
                    reportName           : 'Surefire HTML Report'
                ])
            }
        }
    }

    post {
        always {
            // Archive the raw test evidence and generated reports so they stay attached to the build record.
            archiveArtifacts artifacts: '**/surefire-reports/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/allure-results/**',    allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/allure-report/**',     allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/site/**',              allowEmptyArchive: true

            script {
                // Parse the JUnit XML so Slack can show the exact passed and failed test names.
                def buildStatus = currentBuild.currentResult ?: 'SUCCESS'
                def color = buildStatus == 'SUCCESS' ? 'good' : (buildStatus == 'UNSTABLE' ? 'warning' : 'danger')
                def reportJson = ''
                try {
                    reportJson = powershell(
                        returnStdout: true,
                        script: '''
$files = Get-ChildItem -Path 'target/surefire-reports' -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue
$passed = New-Object System.Collections.Generic.List[string]
$failed = New-Object System.Collections.Generic.List[string]
$skipped = 0
$total = 0

foreach ($file in $files) {
    [xml]$xml = Get-Content $file.FullName
    $suiteName = $xml.testsuite.name

    foreach ($testCase in $xml.testsuite.testcase) {
        $total++
        $displayName = if ($testCase.classname) { "$($testCase.classname).$($testCase.name)" } else { "$suiteName.$($testCase.name)" }

        if ($testCase.skipped) {
            $skipped++
        } elseif ($testCase.failure -or $testCase.error) {
            $failed.Add($displayName)
        } else {
            $passed.Add($displayName)
        }
    }
}

[pscustomobject]@{
    total   = $total
    passed  = $passed
    failed  = $failed
    skipped = $skipped
} | ConvertTo-Json -Depth 4 -Compress
'''
                    ).trim()
                } catch (e) {
                    reportJson = '{"total":0,"passed":[],"failed":[],"skipped":0}'
                }

                def summary = new groovy.json.JsonSlurperClassic().parseText(reportJson)
                def passedTests = summary.passed ?: []
                def failedTests = summary.failed ?: []
                def formatTests = { List tests -> tests ? tests.collect { "• `${it}`" }.join('\n') : '• _None_' }
                def resultEmoji = buildStatus == 'SUCCESS' ? ':white_check_mark:' : (buildStatus == 'UNSTABLE' ? ':warning:' : ':x:')
                def msg = """
${resultEmoji} *Build ${buildStatus}*
*Job:* `${env.JOB_NAME} #${env.BUILD_NUMBER}`
*Branch:* `${env.BRANCH_NAME ?: 'n/a'}`
*Duration:* ${currentBuild.durationString?.replace(' and counting', '') ?: 'n/a'}

*Test summary*
• Total: ${summary.total ?: 0}
• Passed: ${passedTests.size()}
• Failed: ${failedTests.size()}
• Skipped: ${summary.skipped ?: 0}

*Passed tests*
${formatTests(passedTests)}

*Failed tests*
${formatTests(failedTests)}

*Build URL:* ${env.BUILD_URL}
""".stripIndent().trim()

                slackSend(color: color, message: msg)
            }
        }
    }
}
