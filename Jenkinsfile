pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['qa', 'staging', 'production'], description: 'Choose the environment to run tests against')
        choice(name: 'TEST_SUITE', choices: ['regression.xml', 'flight-reservation.xml', 'vendor-portal.xml'], description: 'Choose the suite to run')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Browser for single-browser runs')
        string(name: 'THREAD_COUNT', defaultValue: '2', description: 'Number of parallel threads')
        booleanParam(name: 'RUN_CROSS_BROWSER', defaultValue: false, description: 'Check this box to run on both Chrome and Firefox')
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
    }

    stages {
        stage('Prepare Workspace') {
            steps {
                echo "🧹 Cleaning up old artifacts..."
                sh 'rm -rf target allure-report'
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    echo "📦 Building JAR and preparing resources..."
                    sh 'mvn clean package -DskipTests'

                    echo "🐳 Building Docker Image..."
                    def app = docker.build("${env.IMAGE_NAME}:${env.BUILD_NUMBER}", "--no-cache .")

                    echo "🔐 Logging in and Pushing Docker Images..."
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                        app.push("latest")
                        app.push("${env.BUILD_NUMBER}")
                    }
                }
            }
        }

        stage('Run Tests') {
            matrix {
                axes {
                    axis {
                        name 'BROWSER_AXIS'
                        // If cross-browser is checked, run on both; otherwise, use the selected browser
                        values { params.RUN_CROSS_BROWSER ? ['chrome', 'firefox'] : [params.BROWSER] }
                    }
                }
                stages {
                    stage('Test on ${BROWSER_AXIS}') {
                        steps {
                            script {
                                def projectName = "tests_${BROWSER_AXIS}_${env.BUILD_NUMBER}"
                                try {
                                    echo "🚀 Launching ${params.TEST_SUITE} on ${BROWSER_AXIS}..."
                                    // Using triple-double quotes for a cleaner multi-line shell command
                                    sh """
                                        COMPOSE_PROJECT_NAME=${projectName} \\
                                        ENV=${params.ENV} \\
                                        TEST_SUITE=${params.TEST_SUITE} \\
                                        BROWSER=${BROWSER_AXIS} \\
                                        THREAD_COUNT=${params.THREAD_COUNT} \\
                                        docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations
                                    """
                                } catch (any) {
                                    error("Tests failed for suite ${params.TEST_SUITE} on ${BROWSER_AXIS}.")
                                } finally {
                                    // CRITICAL: Each browser's results are copied to a UNIQUE directory. This prevents all race conditions.
                                    echo "📂 Copying Allure results from ${BROWSER_AXIS} container..."
                                    sh "mkdir -p ./target/allure-results-${BROWSER_AXIS}/"
                                    sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. ./target/allure-results-${BROWSER_AXIS}/ || true"

                                    echo "🧹 Tearing down ${BROWSER_AXIS} test environment..."
                                    sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🧪 Generating Allure Report..."
                // The Allure plugin will automatically find all 'allure-results-*' directories within 'target' and aggregate them.
                // This is the simplest, most robust way to generate the report.
                allure(
                        results: [[path: 'target']],
                        reportBuildPolicy: 'ALWAYS'
                )

                echo "🧹 Final workspace cleanup..."
                cleanWs()

                echo "✅ Pipeline completed successfully."
            }
        }
    }
}