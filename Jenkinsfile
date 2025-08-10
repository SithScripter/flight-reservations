// Jenkinsfile - Simplified, Plugin-Only Approach

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
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    // Always set browsersToTest fresh from params — never reuse leftovers
                    browsersToTest = []

                    if (params.RUN_CROSS_BROWSER) {
                        // Explicit override: always Chrome + Firefox
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        // Take user-selected browser, fallback to Chrome if invalid
                        def selectedBrowser = params.BROWSER?.toLowerCase()
                        browsersToTest = (selectedBrowser in ['chrome', 'firefox']) ? [selectedBrowser] : ['chrome']
                    }

                    // Safety: remove any accidental duplicates
                    browsersToTest = browsersToTest.unique()

                    echo "✅ Browsers to test: ${browsersToTest.join(', ')}"
                }
            }
        }


        stage('Build & Push Docker Image') {
            steps {
                script {
                    sh 'mvn clean package -DskipTests'
                    def app = docker.build("${env.IMAGE_NAME}:${env.BUILD_NUMBER}", "--no-cache .")
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                        app.push("latest")
                        app.push("${env.BUILD_NUMBER}")
                    }
                }
            }
        }

        stage('Run Tests in Parallel') {
            agent none
            steps {
                script {
                    def parallelStages = [:]
                    for (String browser : browsersToTest) {
                        parallelStages["Test on ${browser}"] = {
                            node {
                                deleteDir()
                                checkout scm

                                def projectName = "tests_${browser}_${env.BUILD_NUMBER}"
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
                                    // Store results per browser
                                    sh "mkdir -p target/allure-results-${browser}/"
                                    sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. target/allure-results-${browser}/ || true"

                                    // Stash for aggregation
                                    stash name: "allure-results-${browser}", includes: "target/allure-results-${browser}/**"

                                    sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
                                }
                            }
                        }
                    }
                    parallel parallelStages
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🤝 Aggregating Allure results..."

                // Always unique final dir
                def finalReportDir = 'allure-final-results'
                sh "rm -rf ${finalReportDir} && mkdir -p ${finalReportDir}"

                // Unstash + copy
                for (String browser : browsersToTest) {
                    try {
                        unstash name: "allure-results-${browser}"
                        sh "cp -r target/allure-results-${browser}/* ${finalReportDir}/ || true"
                    } catch (Exception e) {
                        echo "⚠️ No results for ${browser}: ${e}"
                    }
                }

                echo "🧪 Generating Allure Report..."
                allure(
                        results: [[path: finalReportDir]],
                        reportBuildPolicy: 'ALWAYS'
                )
            }
        }
    }

}
