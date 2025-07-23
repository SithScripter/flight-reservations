pipeline{

    agent any

    stages{

        stage('Build Jar'){
            steps(){
                bat 'mvn clean package -DskipTests'
            }
        }

        stage('Build Image'){
            steps(){
                bat 'docker build -t=gaumji19/selenium:latest .'
            }
        }

        stage('push image'){
            environment{
                //assuming you have stored credentials with this name
                DOCKER_HUB = credentials('dockerhub-creds')
            }
            steps(){
                bat 'echo %DOCKER_HUB_PSW% | docker login -u %DOCKER_HUB_USR% --password-stdin'
                bat "docker tag gaumji19/selenium:latest gaumji19/selenium:${env.BUILD_NUMBER}"
                bat "docker push gaumji19/selenium:${env.BUILD_NUMBER}"
            }
        }
    }
    post {
        always {
            bat "docker logout"
        }
    }

}