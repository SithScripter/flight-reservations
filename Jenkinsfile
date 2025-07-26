pipeline {
    agent any

    parameters {
        choice(
            name: 'ACTION',
            choices: ['BUILD_AND_PUSH', 'TEST'],
            description: 'Select the action to perform'
        )
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Build & Push') {
            when { expression { params.ACTION == 'BUILD_AND_PUSH' } }
            steps {
                script {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'dockerhub-creds',
                            usernameVariable: 'DOCKER_HUB_USR',
                            passwordVariable: 'DOCKER_HUB_PSW'
                        )
                    ]) {
                        echo "📦 Building JAR..."
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
            when { expression { params.ACTION == 'TEST' } }
            steps {
                echo "🚀 Launching test environment with Docker Compose..."
                sh """
                    docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations
                """
            }
        }

        stage('Allure Report') {
            when { expression { params.ACTION == 'TEST' } }
            steps {
                echo "🧪 Generating Allure Report..."
                allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
            }
        }
    }

    post {
        // This block runs after all stages
        always {
            echo "📤 Cleaning up..."
            sh 'docker logout || true'
        }
        // This block runs only on success
        success {
            // ✅ THIS IS THE FIX: If the build was successful, trigger the test job
            script {
                if (params.ACTION == 'BUILD_AND_PUSH') {
                    echo "Triggering downstream 'run-tests' job..."
                    build job: 'run-tests', parameters: [
                        string(name: 'ACTION', value: 'TEST')
                    ]
                }
            }
        }
    }
}