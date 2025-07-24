pipeline {
    agent any

    environment {
        IMAGE_NAME = "gaumji19/selenium"
        IMAGE_TAG = "${env.BUILD_NUMBER}"
        DOCKER_HUB = credentials('dockerhub-creds')
    }

    stages {
        stage('Build JAR') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Push to Docker Hub') {
            steps {
                sh '''
                    echo "$DOCKER_HUB_PSW" | docker login -u "$DOCKER_HUB_USR" --password-stdin
                    docker tag ${IMAGE_NAME}:latest ${IMAGE_NAME}:${BUILD_NUMBER}
                    docker push ${IMAGE_NAME}:${BUILD_NUMBER}
                '''
            }
        }

        stage('Run Tests with Allure') {
            steps {
                sh '''
                    echo "Running Tests with Maven..."
                    mvn clean test \
                      -Dbrowser=chrome \
                      -DsuiteXmlFile=test-suites/flight-reservation.xml
                '''
            }
        }

        stage('Allure Report') {
            steps {
                allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
            }
        }
    }

    post {
        always {
        script {
            node {
                sh 'docker logout || true'
                archiveArtifacts artifacts: 'target/allure-results/**/*.*, target/surefire-reports/**/*.*', allowEmptyArchive: true
            }
        }
    }
}
}
