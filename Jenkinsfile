pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['qa', 'staging', 'production'], description: 'Choose the environment')
        choice(name: 'TEST_SUITE', choices: ['regression.xml', 'flight-reservation.xml', 'vendor-portal.xml'], description: 'Suite to run')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Single browser selection')
        string(name: 'THREAD_COUNT', defaultValue: '2', description: 'Number of parallel threads')
        booleanParam(name: 'RUN_CROSS_BROWSER', defaultValue: false, description: 'Run on both Chrome and Firefox')
    }

    environment {
        IMAGE_NAME = "gaumji19/flight-reservations"
    }

    //force multibranch to run this pipeline
    //force multibranch to run this pipeline
    stages {
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

        stage('Run Tests') {
            steps {
                script {
                    def browsersToTest = []
                    if (params.RUN_CROSS_BROWSER) {
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        browsersToTest = [params.BROWSER]
                    }
                    echo "✅ Tests will run on: ${browsersToTest.join(', ')}"

                    sh 'rm -rf target'

                    def parallelStages = [:]
                    for (String browser : browsersToTest) {
                        parallelStages["Test on ${browser}"] = {
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
                                sh "mkdir -p target/allure-results-${browser}/"
                                sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. target/allure-results-${browser}/ || true"
                                sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
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
                echo "🤝 Aggregating Allure results..."

                def finalReportDir = 'allure-final-results'
                sh "rm -rf ${finalReportDir} && mkdir -p ${finalReportDir}"

                // Simple and direct copy. The environment files are already perfect.
                sh "find target/allure-results-* -type f -exec cp {} ${finalReportDir}/ \\; || true"

                echo "🧪 Generating Allure Report..."
                allure(
                        results: [[path: finalReportDir]],
                        reportBuildPolicy: 'ALWAYS'
                )
            }
        }
    }
}