// Define a safe-scoped variable to hold the list of browsers we will test against.
def browsersToTest = []

pipeline {
    agent any

    parameters {
        choice(name: 'ENV', choices: ['qa', 'staging', 'production'], description: 'Choose the environment')
        choice(name: 'TEST_SUITE', choices: ['regression.xml', 'flight-reservation.xml', 'vendor-portal.xml'], description: 'Suite to run')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox'], description: 'Single browser selection')
        string(name: 'THREAD_COUNT', defaultValue: '2', description: 'Parallel threads')
        booleanParam(name: 'RUN_CROSS_BROWSER', defaultValue: false, description: 'Run on both Chrome and Firefox')
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
                        // Defensive check to handle any wrong or unexpected param values
                        def selectedBrowser = params.BROWSER?.toLowerCase()
                        if (selectedBrowser == 'chrome' || selectedBrowser == 'firefox') {
                            browsersToTest = [selectedBrowser]
                        } else {
                            // default fallback (optional)
                            browsersToTest = ['chrome']
                        }
                    }
                    // Remove duplicates just in case
                    browsersToTest = browsersToTest.unique()
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
                                // Clean the workspace completely for isolation
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

                                    // Only stash if results files actually exist
                                    def resultCount = sh(script: "ls target/allure-results-${browser}/ 2>/dev/null | wc -l", returnStdout: true).trim()
                                    if (resultCount != "0") {
                                        stash name: "allure-results-${browser}", includes: "target/allure-results-${browser}/**"
                                    } else {
                                        echo "No Allure results were found for ${browser} to stash."
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

                // Clean and prepare final target directory for Allure
                sh "rm -rf target && mkdir -p target"

                // Unstash the results for browsers that successfully stashed
                for (String browser : browsersToTest) {
                    try {
                        unstash name: "allure-results-${browser}"
                    } catch (Exception e) {
                        echo "Could not find stash for ${browser}. It may have failed or produced no results."
                    }
                }

                // Merge all results into one folder for Allure processing
                sh "mkdir -p target/allure-results"
                sh "find target/allure-results-* -type f -exec cp {} target/allure-results/ \\; || true"

                // Debug output for troubleshooting
                echo "🧪 Debug: Listing final Allure results directory contents:"
                sh "ls -l target/allure-results || true"

                // Generate the Allure report
                allure(
                        results: [[path: 'target/allure-results']],
                        reportBuildPolicy: 'ALWAYS'
                )

                echo "✅ Pipeline completed successfully."
            }
        }
    }
}
