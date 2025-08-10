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

//        stage('Prepare Workspace') {
//            steps {
//                echo "🧹 Cleaning up old artifacts..."
//                sh 'rm -rf target allure-report'
//            }
//        }

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

        // This stage now orchestrates the parallel test runs.
        stage('Run Tests in Parallel') {
            // ✅ FIX: The 'agent none' ensures this stage only orchestrates the parallel steps.
            agent none
            steps {
                script {
                    def parallelStages = [:]
                    for (String browser : browsersToTest) {
                        parallelStages["Test on ${browser}"] = {
                            // Each parallel stage gets its own node, ensuring a clean slate.
                            node {
                                // Keep the main workspace clean for the final report.
//                                sh 'rm -rf target allure-report'
                                sh 'rm -rf *'

                                // Checkout code into the executor's default workspace.
                                checkout scm

                                def projectName = "tests_${browser}_${env.BUILD_NUMBER}"
                                try {
                                    echo "🚀 Launching ${params.TEST_SUITE} on ${browser}..."
                                    // The docker-compose file is now at the root of this workspace.
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

                                    // ✅ FIX: Stash the results with a unique name for each browser.
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
                // ✅ FIX: Unstash all results into the current workspace.
                echo "🤝 Aggregating Allure results from all parallel runs..."
                sh "rm -rf target"
                sh "mkdir -p target"

                // Unstash results from every browser that ran.
                for (String browser : browsersToTest) {
                    unstash name: "allure-results-${browser}"
                }

                // Now merge the unstashed results into a single directory.
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