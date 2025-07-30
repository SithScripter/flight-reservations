pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timeout(time: 20, unit: 'MINUTES')
    }

    parameters {
        choice(
            name: 'ACTION',
            choices: ['TEST', 'BUILD_AND_PUSH'],
            description: 'Select the action to perform'
        )
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Guard Job Misuse') {
            steps {
                script {
                    def jobName = env.JOB_NAME ?: ''
                    if (jobName.toLowerCase().contains("run-tests") && params.ACTION == 'BUILD_AND_PUSH') {
                        error("❌ This job (${jobName}) is not allowed to perform BUILD_AND_PUSH. Use the 'build-and-push' job instead.")
                    }
                }
            }
        }

        stage('Build & Push') {
            when { expression { params.ACTION == 'BUILD_AND_PUSH' } }
            steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-creds',
                        usernameVariable: 'DOCKER_HUB_USR',
                        passwordVariable: 'DOCKER_HUB_PSW'
                    )]) {
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
                echo "🗂️ Creating target directory on Jenkins host..."
                sh 'mkdir -p target/allure-results && chmod -R 777 target'

                echo "🚀 Launching test environment..."
                sh "docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations"
            }
        }
    }

    post {
        always {
            script {
                echo "📤 Cleaning up..."

                if (params.ACTION == 'TEST') {
                    echo "🧪 Generating Allure Report..."
                    try {
                        sh '''
                            echo "Current Jenkins Workspace: $(pwd)"
                            echo "Contents of workspace:"
                            ls -la .
                            echo "Contents of target:"
                            ls -la target/ || true
                            echo "Contents of target/allure-results:"
                            ls -la target/allure-results/ || true
                            echo "Permissions of target/allure-results:"
                            stat -c '%a %n' target/allure-results/ || true
                        '''

                        allure(
                            tool: 'Allure_2.34.1',
                            includeProperties: false,
                            jdk: '',
                            results: [[path: 'target/allure-results']]
                        )
                    } catch (e) {
                        echo "⚠️ Allure report generation failed: ${e.getMessage()}"
                    }

                    echo "📦 Archiving test reports..."
                    archiveArtifacts artifacts: 'target/allure-results/**/*.*, target/surefire-reports/**/*.*', allowEmptyArchive: true

                    echo "🧹 Tearing down test environment..."
                    sh "docker-compose -f docker-compose.test.yml down -v"
                }

                sh 'docker logout || true'
                cleanWs()
            }
        }

        success {
            script {
                if (params.ACTION == 'BUILD_AND_PUSH') {
                    def downstreamJobName = 'run-tests'
                    try {
                        def downstreamJob = Jenkins.instance.getItemByFullName(downstreamJobName)
                        def isRunning = downstreamJob?.isBuilding()

                        if (isRunning) {
                            echo "🛑 Skipping trigger: '${downstreamJobName}' is already running."
                        } else {
                            echo "✅ Triggering downstream job: '${downstreamJobName}'..."
                            build job: downstreamJobName, parameters: [
                                string(name: 'ACTION', value: 'TEST')
                            ]
                        }
                    } catch (err) {
                        echo "⚠️ Error while checking/triggering downstream job: ${err.getMessage()}"
                    }
                }
            }
        }
    }
}
