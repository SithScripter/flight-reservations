pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
    }

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
                echo "Pre-creating target directory on Jenkins host..."
                sh 'mkdir -p target/allure-results'
                sh 'chmod 777 target'
                sh 'chmod 777 target/allure-results'

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
                        sh 'echo \"Current Jenkins Workspace: $(pwd)\"'
                        sh 'echo \"Contents of Jenkins workspace:\"'
                        sh 'ls -la .'
                        sh 'echo \"Contents of Jenkins workspace target directory:\"'
                        sh 'ls -la target/ || true'
                        sh 'echo \"Contents of Jenkins workspace target/allure-results directory:\"'
                        sh 'ls -la target/allure-results/ || true'
                        sh 'echo \"Permissions of Jenkins workspace target/allure-results directory:\"'
                        sh 'stat -c \'%a %n\' target/allure-results/ || true'
                        echo '-------------------------------------------------'

                        allure(
                            tool: 'Allure_2.34.1',
                            includeProperties: false,
                            jdk: '',
                            results: [[path: 'target/allure-results']]
                        )
                    } catch (e) {
                        echo "Allure report generation failed. Error: ${e.getMessage()}"
                    }

                    echo "📦 Archiving reports..."
                    archiveArtifacts artifacts: 'target/allure-results/**/*.*, target/surefire-reports/**/*.*', allowEmptyArchive: true

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
                    def downstreamJobName = 'run-tests'
                    def running = Jenkins.instance.getItemByFullName(downstreamJobName)?.isBuilding()

                    if (running) {
                        echo "🛑 Skipping downstream trigger. Job '${downstreamJobName}' is already running."
                    } else {
                        echo "✅ Triggering downstream 'run-tests' job..."
                        build job: downstreamJobName, parameters: [
                            string(name: 'ACTION', value: 'TEST')
                        ]
                    }
                }
            }
        }
    }
}
