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
                // Parse failed test names from JUnit XML results
                def failedTests = ''
                try {
                    def xmlFiles = findFiles(glob: '**/surefire-reports/TEST-*.xml')
                    def failures = []
                    xmlFiles.each { f ->
                        def xml = readFile(f.path)
                        def matcher = xml =~ /testname="([^"]+)"[^>]*>[\s\S]*?<failure/
                        matcher.each { m -> failures << m[1] }
                        // Also catch <testcase name="..." ...><failure pattern
                        def m2 = xml =~ /<testcase[^>]+name="([^"]+)"[^>]*>[\s\S]*?<failure/
                        m2.each { m -> failures << m[1] }
                    }
                    if (failures) {
                        failedTests = "\nFailed tests:\n" + failures.unique().collect { "  • ${it}" }.join("\n")
                    }
                } catch (e) {
                    failedTests = ''
                }

                def buildStatus = currentBuild.currentResult ?: 'UNKNOWN'
                def color = buildStatus == 'SUCCESS' ? 'good' : 'danger'
                def msg = "${buildStatus}: Job '${env.JOB_NAME}' #${env.BUILD_NUMBER}${failedTests}\n${env.BUILD_URL}"

                catchError(buildResult: currentBuild.currentResult) {
                    slackSend(color: color, message: msg)
                }
            }
        }
    }
}
