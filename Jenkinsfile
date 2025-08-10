// Define a safe-scoped variable to hold the list of browsers we will test against.
def browsersToTest = []

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

    stages {
        stage('Initialize') {
            steps {
                script {
                    // Priority: if cross-browser is true, always run on both browsers
                    if (params.RUN_CROSS_BROWSER.toString() == 'true') {
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        def selectedBrowser = params.BROWSER?.toLowerCase()
                        if (selectedBrowser == 'chrome' || selectedBrowser == 'firefox') {
                            browsersToTest = [selectedBrowser]
                        } else {
                            // Default fallback
                            browsersToTest = ['chrome']
                        }
                    }
                    // Ensure unique entries
                    browsersToTest = browsersToTest.unique()
                    echo "Tests will run on the following browsers: ${browsersToTest.join(', ')}"
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
                                // Start fresh workspace for this parallel stage
                                deleteDir()
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

                                    // Only stash if results exist
                                    def resultCount = sh(script: "ls target/allure-results-${browser}/ 2>/dev/null | wc -l", returnStdout: true).trim()
                                    if (resultCount != "0") {
                                        stash name: "allure-results-${browser}", includes: "target/allure-results-${browser}/**"
                                    } else {
                                        echo "No Allure results found for ${browser}, skipping stash."
                                    }

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

                // Clean/prepare final directory for Allure results
                sh "rm -rf target && mkdir -p target"

                // Unstash results from each browser safely
                for (String browser : browsersToTest) {
                    try {
                        unstash name: "allure-results-${browser}"
                    } catch (Exception e) {
                        echo "Could not find stash for browser ${browser}: ${e}"
                    }
                }

                // Merge all results into single folder for Allure report generation
                sh "mkdir -p target/allure-results"
                sh "find target/allure-results-* -type f -exec cp -t target/allure-results {} + || true"

                // Debug: list merged results
                echo "🧪 Final Allure results directory contents:"
                sh "ls -l target/allure-results || true"

                // Generate Allure report with correct path
                allure(
                        results: [[path: 'target/allure-results']],
                        reportBuildPolicy: 'ALWAYS'
                )

                echo "✅ Pipeline completed successfully."
            }
        }
    }
}
