pipeline {
    // Run on any available Jenkins executor/agent node.
    agent any

    // Auto-provision configured tool installations for this pipeline run.
    tools {
        maven 'Maven 3.9'
        jdk 'JDK 17'
    }

    // Trigger this pipeline when GitHub sends a push webhook event.
    triggers {
        githubPush()
    }

    // Ordered build lifecycle; each stage is visible in Jenkins Stage View.
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
                // buildResult='UNSTABLE' marks overall build yellow; stageResult='FAILURE' marks this stage red.
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
                    // allowMissing=true avoids hard failure if report generation is skipped/empty.
                    allowMissing         : true,
                    // alwaysLinkToLastBuild keeps a stable link to the newest report.
                    alwaysLinkToLastBuild: true,
                    // keepAll=true preserves report history per build.
                    keepAll              : true,
                    reportDir            : 'target/site',
                    reportFiles          : 'surefire-report.html',
                    reportName           : 'Surefire HTML Report'
                ])
            }
        }
    }

    // Post actions run after stages complete, regardless of success/failure.
    post {
        always {
            // Archive the raw test evidence and generated reports so they stay attached to the build record.
            archiveArtifacts artifacts: '**/surefire-reports/*.xml', allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/allure-results/**',    allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/allure-report/**',     allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/site/**',              allowEmptyArchive: true

            script {
                // script { } allows imperative Groovy inside declarative pipeline syntax.
                // Parse JUnit XML for summary counts and failed test names for Slack.
                def buildStatus = currentBuild.currentResult ?: 'SUCCESS'
                def color = buildStatus == 'SUCCESS' ? 'good' : (buildStatus == 'UNSTABLE' ? 'warning' : 'danger')
                def reportText = ''
                try {
                    // Use PowerShell on Windows agents to aggregate all surefire XML testcases.
                    reportText = powershell(
                        returnStdout: true,
                        script: '''
$files = Get-ChildItem -Path 'target/surefire-reports' -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue
$passedCount = 0
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
            $passedCount++
        }
    }
}

$summaryLines = @(
    "TOTAL=$total"
    "PASSED=$passedCount"
    "FAILED=$($failed.Count)"
    "SKIPPED=$skipped"
    "FAILED_LIST=$(if ($failed.Count -gt 0) { $failed -join ' || ' } else { '__NONE__' })"
)

$summaryLines -join "`n"
'''
                    ).trim()
                } catch (e) {
                    reportText = 'TOTAL=0\nPASSED=0\nFAILED=0\nSKIPPED=0\nFAILED_LIST=__NONE__'
                }

                def summary = [:]
                // Convert KEY=VALUE lines into a Groovy map for message templating.
                reportText.readLines().each { line ->
                    def idx = line.indexOf('=')
                    if (idx > 0) {
                        summary[line.substring(0, idx)] = line.substring(idx + 1)
                    }
                }

                def failedTests = (summary.FAILED_LIST ?: '__NONE__') == '__NONE__' ? [] : (summary.FAILED_LIST.split(/\s*\|\|\s*/) as List)
                def formatTests = { List tests -> tests.collect { "• `${it}`" }.join('\n') }
                def failedTestsSection = failedTests ? "\n\n*Failed tests*\n${formatTests(failedTests)}" : ''
                def resultEmoji = buildStatus == 'SUCCESS' ? ':white_check_mark:' : (buildStatus == 'UNSTABLE' ? ':warning:' : ':x:')
                def msg = """
${resultEmoji} *Build ${buildStatus}*
*Job:* `${env.JOB_NAME} #${env.BUILD_NUMBER}`
*Branch:* `${env.BRANCH_NAME ?: 'n/a'}`
*Duration:* ${currentBuild.durationString?.replace(' and counting', '') ?: 'n/a'}

*Test summary*
• Total: ${summary.TOTAL ?: 0}
• Passed: ${summary.PASSED ?: 0}
• Failed: ${summary.FAILED ?: 0}
• Skipped: ${summary.SKIPPED ?: 0}
${failedTestsSection}

*Build URL:* ${env.BUILD_URL}
""".stripIndent().trim()

                try {
                    // slackSend comes from the Slack plugin; catches errors so notification issues do not fail the build.
                    slackSend(color: color, message: msg)
                } catch (notifyErr) {
                    echo "Slack notification failed: ${notifyErr}"
                }
            }
        }
    }
}
