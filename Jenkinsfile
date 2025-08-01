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
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-creds',
                            usernameVariable: 'DOCKER_HUB_USR',
                            passwordVariable: 'DOCKER_HUB_PSW'
                        )
                    ]) {
                        echo "📦 Building JAR and preparing resources..."
                        sh 'mvn clean package -DskipTests'

                        echo "🐳 Building Docker Image..."
                        sh "docker build -t ${IMAGE_NAME}:latest -t ${IMAGE_NAME}:${IMAGE_TAG} ."

                        echo "🚀 Pushing to Docker Hub..."
                        sh "echo '${DOCKER_HUB_PSW}' | docker login -u '${DOCKER_HUB_USR}' --password-stdin"
                        sh "docker push ${IMAGE_NAME}:latest"
                        sh "docker push ${IMAGE_NAME}:${IMAGE_TAG}"
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
                        echo "Test container finished with a non-zero exit code."
                        currentBuild.result = 'FAILURE'
                    }
                }

                echo "📂 Copying Allure results from container..."
                sh "docker cp flight-reservations-tests:/home/flight-reservations/target/allure-results/. ./target/allure-results/"

                script {
                    if (currentBuild.result == 'FAILURE') {
                        error("Tests failed. See test logs and Allure report for details.")
                    }
                }
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
                sh 'docker logout || true'
                cleanWs()
            }
        }
    }
}