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

//         stage('Run Tests in Container') {
//             when { expression { params.ACTION == 'TEST' } }
//             steps {
//                 echo "🐳 Pulling Docker image and running tests..."
//                 sh """
//                     docker pull ${IMAGE_NAME}:latest
//                     docker run --rm \\
//                         -v "\${PWD}/target:/home/flight-reservations/target" \\
//                         ${IMAGE_NAME}:latest \\
//                         sh -c 'mvn test'
//                 """
//             }
//         }

        stage('Run Tests in Container') {
            when { expression { params.ACTION == 'TEST' } }
            steps {
                echo "🚀 Launching test environment with Docker Compose..."
                sh """
                    docker-compose -f docker-compose.test.yml up --build --exit-code-from flight-reservations
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
        always {
            echo "📤 Cleaning up Docker credentials..."
            sh 'docker logout || true'

            script {
                if (params.ACTION == 'TEST') {
                    echo "📦 Archiving reports..."
                    archiveArtifacts artifacts: 'target/allure-results/**/*.*, target/surefire-reports/**/*.*', allowEmptyArchive: true
                }
            }
        }
    }
}
