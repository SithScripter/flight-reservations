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
                echo "Pre-creating target directory on Jenkins host..."
                sh 'mkdir -p target/allure-results'
                sh 'chmod 777 target' // Ensure Jenkins has write permissions to 'target'
                sh 'chmod 777 target/allure-results' // Ensure Jenkins has write permissions to 'allure-results'

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
                        // Host diagnostics BEFORE allure command - (your existing diagnostics)
                        sh 'echo "Current Jenkins Workspace: $(pwd)"'
                        sh 'echo "Contents of Jenkins workspace:"'
                        sh 'ls -la .'
                        sh 'echo "Contents of Jenkins workspace target directory:"'
                        sh 'ls -la target/ || true'
                        sh 'echo "Contents of Jenkins workspace target/allure-results directory:"'
                        sh 'ls -la target/allure-results/ || true'
                        sh 'echo "Permissions of Jenkins workspace target/allure-results directory:"'
                        sh 'stat -c \'%a %n\' target/allure-results/ || true'
                        echo '-------------------------------------------------'

                        // ✅ FIX: Specify the 'tool' attribute here with the name you gave in Global Tool Configuration
                        allure(
                            tool: 'Allure_2.34.1', // Use the name you configured in Jenkins Global Tool Configuration
                            includeProperties: false,
                            jdk: '', // or 'jdk8' if you have one configured
                            results: [[path: 'target/allure-results']]
                        )
                    } catch (e) {
                        echo "Allure report generation failed. Error: ${e.getMessage()}"
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