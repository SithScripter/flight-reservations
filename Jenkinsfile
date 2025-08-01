pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['qa', 'staging', 'production'], description: 'Choose the environment to run tests against')
        choice(name: 'TEST_SUITE', choices: ['regression.xml', 'flight-reservation.xml', 'vendor-portal.xml'], description: 'Choose the suite to run')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Browser to run tests')
        string(name: 'THREAD_COUNT', defaultValue: '2', description: 'Number of parallel threads')
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
                stage('Build & Push') {
                    steps {
                        script {
                            echo "📦 Building JAR and preparing resources..."
                            sh 'mvn clean package -Dmaven.test.skip=true'

                            echo "🐳 Building Docker Image..."
                            def app = docker.build("${env.IMAGE_NAME}:${env.IMAGE_TAG}", ".")

                            echo "🔐 Logging in and Pushing Docker Images..."
                            docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                                app.push("latest")
                                app.push("${env.IMAGE_TAG}")
                            }
                        }
                    }
                }

        stage('Run Tests in Container') {
            steps {
                echo "🗂️ Creating target directory on Jenkins host..."
                sh 'mkdir -p target/allure-results'
                sh 'chmod -R 777 target'

                echo "🚀 Launching test environment with the following parameters:"
                echo "   Environment: ${params.ENV}"
                echo "   Test Suite: ${params.TEST_SUITE}"
                echo "   Browser: ${params.BROWSER}"
                echo "   Thread Count: ${params.THREAD_COUNT}"

                script {
                    def command = "ENV=${params.ENV} TEST_SUITE=${params.TEST_SUITE} BROWSER=${params.BROWSER} THREAD_COUNT=${params.THREAD_COUNT} docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations"
                    try {
                        sh command
                    } catch (any) {
                        error("Tests failed. The test container exited with a non-zero code.")
                    }
                }

                echo "📂 Copying Allure results from container..."
                sh "docker cp flight-reservations-tests:/home/flight-reservations/target/allure-results/. ./target/allure-results/ || true"

            }
        }
    }

    post {
        always {
            script {
                echo "🧪 Generating Allure Report..."
                sh 'ls -la target/allure-results/ || true'

                allure(
                    results: [[path: 'target/allure-results']]
                )

                echo "🧹 Tearing down test environment..."
                sh "docker-compose -f docker-compose.test.yml down -v || true"

                echo "📤 Cleaning up..."
                cleanWs()

                echo "✅ Pipeline completed."
            }
        }
    }
}