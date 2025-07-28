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
                sh "docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations"
            }
        }
    }

    post {
        always {
            script {
                if (params.ACTION == 'TEST') {
                    echo "🧪 Generating Allure Report..."
                    try {
                        // Host diagnostics BEFORE allure command - CORRECTED SYNTAX
                        echo '--- JENKINS HOST DIAGNOSTICS (BEFORE ALLURE) ---'
                        sh 'echo "Current Jenkins Workspace: $(pwd)"'
                        sh 'echo "Contents of Jenkins workspace:"'
                        sh 'ls -la .'
                        sh 'echo "Contents of Jenkins workspace target directory:"'
                        sh 'ls -la target/ || true' // || true prevents build failure if dir doesn't exist
                        sh 'echo "Contents of Jenkins workspace target/allure-results directory:"'
                        sh 'ls -la target/allure-results/ || true'
                        sh 'echo "Permissions of Jenkins workspace target/allure-results directory:"'
                        sh 'stat -c \'%a %n\' target/allure-results/ || true' // Escaping single quotes for Groovy
                        echo '-------------------------------------------------'

                        allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
                    } catch (e) {
                        echo "Allure report generation failed, likely no results found. Error: ${e.getMessage()}"
                        // Optionally rethrow if you want the build to fail on Allure report gen failure
                        // throw e
                    }
                    echo "📦 Archiving reports..."
                    archiveArtifacts artifacts: 'target/allure-results/**/*.*, target/surefire-reports/**/*.*', allowEmptyArchive: true

                    // ✅ THIS IS THE FIX: Tear down the test environment
                    echo "Tearing down test environment..."
                    sh "docker-compose -f docker-compose.test.yml down -v"
                }
            }

            echo "📤 Cleaning up..."
            sh 'docker logout || true'
            cleanWs()
        }

        success {
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