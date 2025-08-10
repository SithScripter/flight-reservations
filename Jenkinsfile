def browsersToTest = []

pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['qa', 'staging', 'production'], description: 'Choose the environment to run tests against')
        choice(name: 'TEST_SUITE', choices: ['regression.xml', 'flight-reservation.xml', 'vendor-portal.xml'], description: 'Choose the suite to run')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Browser for single-browser runs')
        string(name: 'THREAD_COUNT', defaultValue: '2', description: 'Number of parallel threads')
        booleanParam(name: 'RUN_CROSS_BROWSER', defaultValue: false, description: 'Check this box to run on both Chrome and Firefox')
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    if (params.RUN_CROSS_BROWSER) {
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        browsersToTest = [params.BROWSER]
                    }
                    echo "Tests will run on the following browsers: ${browsersToTest}"
                }
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    sh 'mvn clean package -DskipTests'
                    def app = docker.build("${env.IMAGE_NAME}:${env.BUILD_NUMBER}", "--no-cache .")
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                        app.push("latest")
                        app.push("${env.BUILD_NUMBER}")
                    }
                }
            }
        }

        stage('Run Tests in Parallel') {
            agent none
            steps {
                script {
                    def parallelStages = [:]
                    for (String browser : browsersToTest) {
                        parallelStages["Test on ${browser}"] = {
                            node {
                                sh 'rm -rf *'
                                checkout scm

                                def projectName = "tests_${browser}_${env.BUILD_NUMBER}"
                                try {
                                    sh """
                                        COMPOSE_PROJECT_NAME=${projectName} \\
                                        ENV=${params.ENV} \\
                                        TEST_SUITE=${params.TEST_SUITE} \\
                                        BROWSER=${browser} \\
                                        THREAD_COUNT=${params.THREAD_COUNT} \\
                                        docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations
                                    """
                                } finally {
                                    echo "Stashing Allure results from ${browser} container..."
                                    sh "mkdir -p target/allure-results-${browser}/"
                                    sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. target/allure-results-${browser}/ || true"

                                    stash name: "allure-results-${browser}", includes: "target/allure-results-${browser}/**"

                                    sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
                                }
                            }
                        }
                    }
                    parallel parallelStages
                }
            }
        }
    }

    post {
        always {
            script {
                echo "🤝 Aggregating Allure results from all parallel runs..."
                sh "rm -rf target"
                sh "mkdir -p target"

                // ✅ FIX: Wrap unstash in a try-catch block to prevent build failure if a stash is missing.
                for (String browser : browsersToTest) {
                    try {
                        unstash name: "allure-results-${browser}"
                    } catch (e) {
                        echo "Could not find stash for ${browser}. It may have failed."
                    }
                }

                sh "mkdir -p target/allure-results"
                sh "find target/allure-results-* -type f -exec cp {} target/allure-results/ \\; || true"

                echo "🧪 Generating Allure Report..."
                allure(
                        results: [[path: 'target/allure-results']],
                        reportBuildPolicy: 'ALWAYS'
                )

                echo "✅ Pipeline completed successfully."
            }
        }
    }
}