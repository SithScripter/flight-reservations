pipeline {
    agent any

    parameters {
        choice(name: 'ACTION', choices: ['BUILD_AND_PUSH', 'TEST'], description: 'What should this pipeline do?')
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        DOCKER_HUB = credentials('dockerhub-creds')
    }

    stages {
        stage('Build JAR') {
            when { expression { params.ACTION == 'BUILD_AND_PUSH' } }
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            when { expression { params.ACTION == 'BUILD_AND_PUSH' } }
            steps {
                sh "docker build -t ${IMAGE_NAME}:latest -t ${IMAGE_NAME}:${IMAGE_TAG} ."
            }
        }

        stage('Push to Docker Hub') {
            when { expression { params.ACTION == 'BUILD_AND_PUSH' } }
            steps {
                sh """
                    echo "$DOCKER_HUB_PSW" | docker login -u "$DOCKER_HUB_USR" --password-stdin
                    docker push ${IMAGE_NAME}:latest
                    docker push ${IMAGE_NAME}:${IMAGE_TAG}
                """
            }
        }

        stage('Run Tests in Container') {
            when { expression { params.ACTION == 'TEST' } }
            steps {
                sh """
                    docker pull ${IMAGE_NAME}:latest
                    docker run --rm \
                        -v $PWD/target:/app/target \
                        ${IMAGE_NAME}:latest \
                        sh -c 'mvn test -Dbrowser=chrome -DsuiteXmlFile=src/test/resources/test-suites/flight-reservation.xml'
                """
            }
        }

        stage('Allure Report') {
            when { expression { params.ACTION == 'TEST' } }
            steps {
                allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
            archiveArtifacts artifacts: 'target/allure-results/**/*.*, target/surefire-reports/**/*.*', allowEmptyArchive: true
        }
    }
}
