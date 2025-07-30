pipeline {
    agent any

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

                echo "🚀 Launching test environment..."
                sh "docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations"
            }
        }
    }

    post {
        always {
            script {
                echo "🧪 Generating Allure Report..."
                // Host diagnostics to ensure results are present
                sh 'echo "--- Jenkins Workspace Diagnostics ---"'
                sh 'ls -la target/allure-results/ || true'

                allure(
                    tool: 'Allure_2.34.1',
                    results: [[path: 'target/allure-results']]
                )

                echo "📦 Archiving reports..."
                archiveArtifacts artifacts: 'target/surefire-reports/**/*.*', allowEmptyArchive: true

                echo "🧹 Tearing down test environment..."
                sh "docker-compose -f docker-compose.test.yml down -v || true"

                echo "📤 Cleaning up..."
                sh 'docker logout || true'
                cleanWs()
            }
        }
    }
}