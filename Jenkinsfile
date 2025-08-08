// Define a variable to hold the list of browsers we will test against.
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
                    echo "Determining which browsers to test..."
                    if (params.RUN_CROSS_BROWSER) {
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        browsersToTest = [params.BROWSER]
                    }
                    echo "Tests will run on the following browsers: ${browsersToTest}"
                }
            }
        }

        stage('Prepare Workspace') {
            steps {
                echo "🧹 Cleaning up old artifacts..."
                sh 'rm -rf target allure-report'
            }
        }

        stage('Build & Push Docker Image') {
            steps {
                script {
                    echo "📦 Building JAR and preparing resources..."
                    sh 'mvn clean package -DskipTests'

                    echo "🐳 Building Docker Image..."
                    def app = docker.build("${env.IMAGE_NAME}:${env.BUILD_NUMBER}", "--no-cache .")

                    echo "🔐 Logging in and Pushing Docker Images..."
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                        app.push("latest")
                        app.push("${env.BUILD_NUMBER}")
                    }
                }
            }
        }

        // ✅ REVISED STAGE: Using the standard 'parallel' step for maximum reliability.
        stage('Run Tests') {
            steps {
                script {
                    // Create an empty map to hold our dynamically generated parallel stages.
                    def parallelStages = [:]

                    // Loop through the list of browsers determined in the 'Initialize' stage.
                    for (String browser : browsersToTest) {
                        // The key of the map becomes the name of the parallel stage.
                        parallelStages["Test on ${browser}"] = {
                            // The value of the map is a closure containing the steps for that stage.
                            def projectName = "tests_${browser}_${env.BUILD_NUMBER}"
                            try {
                                echo "🚀 Launching ${params.TEST_SUITE} on ${browser}..."
                                sh """
                                    COMPOSE_PROJECT_NAME=${projectName} \\
                                    ENV=${params.ENV} \\
                                    TEST_SUITE=${params.TEST_SUITE} \\
                                    BROWSER=${browser} \\
                                    THREAD_COUNT=${params.THREAD_COUNT} \\
                                    docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations
                                """
                            } catch (any) {
                                error("Tests failed for suite ${params.TEST_SUITE} on ${browser}.")
                            } finally {
                                echo "📂 Copying Allure results from ${browser} container..."
                                sh "mkdir -p ./target/allure-results-${browser}/"
                                sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. ./target/allure-results-${browser}/ || true"

                                echo "🧹 Tearing down ${browser} test environment..."
                                sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
                            }
                        }
                    }
                    // This command executes all the stages we defined in the map in parallel.
                    parallel parallelStages
                }
            }
        }
    }
    //force jenkins mulibranch to detect changes in the repository
    post {
        always {
            script {
                // Merge allure-results from all browsers into a single target/allure-results
                sh "rm -rf target/allure-results"
                sh "mkdir -p target/allure-results"
                sh "find target/allure-results-* -type f -exec cp {} target/allure-results/ \\; || true"

                echo "🧪 Generating Allure Report..."
                allure(
                        results: [[path: 'target/allure-results']],
                        reportBuildPolicy: 'ALWAYS'
                )

                echo "🧹 Final workspace cleanup..."
                cleanWs()
                echo "✅ Pipeline completed successfully."
            }
        }
    }
}