// Jenkinsfile - Simple & Final QA-Friendly Version
def browsersToTest = []

pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['qa', 'staging', 'production'], description: 'Choose the environment')
        choice(name: 'TEST_SUITE', choices: ['regression.xml', 'flight-reservation.xml', 'vendor-portal.xml'], description: 'Suite to run')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Single browser selection')
        string(name: 'THREAD_COUNT', defaultValue: '2', description: 'Number of parallel threads')
        booleanParam(name: 'RUN_CROSS_BROWSER', defaultValue: false, description: 'Run on both Chrome and Firefox')
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
        ALLURE_CMD = tool 'Allure' // Jenkins configured Allure CLI tool
    }

    stages {

        stage('Initialize') {
            steps {
                script {
                    if (params.RUN_CROSS_BROWSER) {
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        def sel = params.BROWSER?.toLowerCase()
                        browsersToTest = (sel in ['chrome', 'firefox']) ? [sel] : ['chrome']
                    }
                    echo "✅ Browsers to test: ${browsersToTest.join(', ')}"
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    sh 'mvn clean package -DskipTests'
                    def app = docker.build("${IMAGE_NAME}:${BUILD_NUMBER}", "--no-cache .")
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                        app.push("latest")
                        app.push("${BUILD_NUMBER}")
                    }
                }
            }
        }

        stage('Run Tests in Parallel') {
            agent none
            steps {
                script {
                    def runs = [:]
                    for (String browser : browsersToTest) {
                        runs["${browser}"] = {
                            node {
                                deleteDir()
                                checkout scm
                                def projectName = "tests_${browser}_${BUILD_NUMBER}"
                                try {
                                    sh """
                                        COMPOSE_PROJECT_NAME=${projectName} \\
                                        ENV=${params.ENV} \\
                                        TEST_SUITE=${params.TEST_SUITE} \\
                                        BROWSER=${browser} \\
                                        THREAD_COUNT=${params.THREAD_COUNT} \\
                                        docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations
                                    """
                                } finally {
                                    sh "mkdir -p target/allure-results-${browser}"
                                    sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. target/allure-results-${browser}/ || true"

                                    if (sh(script: "ls target/allure-results-${browser} | wc -l", returnStdout: true).trim() != "0") {
                                        stash name: "results-${browser}", includes: "target/allure-results-${browser}/**"
                                    }
                                    sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
                                }
                            }
                        }
                    }
                    parallel runs
                }
            }
        }
    }

    post {
        always {
            script {
                def finalDir = 'allure-final-results'
                sh "rm -rf ${finalDir} && mkdir -p ${finalDir}"

                // Unstash and copy all browser results
                for (String browser : browsersToTest) {
                    try {
                        unstash "results-${browser}"
                        sh "cp -r target/allure-results-${browser}/* ${finalDir}/ || true"
                    } catch (e) {
                        echo "⚠️ No results for ${browser}"
                    }
                }

                // Merge environment.properties
                writeFile file: "${finalDir}/environment.properties", text: mergeEnvProps(browsersToTest)

                // Generate Allure report with CLI
                sh "${ALLURE_CMD}/bin/allure generate ${finalDir} -o allure-report --clean"

                // Publish report in Jenkins
                allure includeProperties: false, report: 'allure-report'
            }
        }
    }
}

// ---------- Helper to merge environment.properties ----------
@NonCPS
String mergeEnvProps(List<String> browsers) {
    def lines = []
    def common = [
            "Test.Suite=${params.TEST_SUITE}",
            "Thread.Count=${params.THREAD_COUNT}",
            "ENV=${params.ENV}",
            "Java.Version=${System.getProperty('java.version')}",
            "OS=${System.getProperty('os.name')}",
            "Selenium.Grid=true"
    ]
    if (browsers.size() > 1) {
        browsers.each { b ->
            lines << "Browser.${b}=${b.capitalize()}"
            lines << "Browser.Version.${b}=unknown" // your test code can overwrite this
        }
        lines.addAll(common)
    } else {
        def b = browsers[0]
        lines << "Browser=${b.capitalize()}"
        lines << "Browser.Version=unknown"
        lines.addAll(common)
    }
    return lines.join("\n") + "\n"
}
