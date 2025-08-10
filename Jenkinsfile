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
                    // Always set browsersToTest to guarantee what is used in post block.
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
                                // Always clean only own target folder, not entire workspace.
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
                                    def filesInResult = sh(script: "ls target/allure-results-${browser} 2>/dev/null | wc -l", returnStdout: true).trim()
                                    if (filesInResult != "0") {
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
                sh "rm -rf target"
                sh "mkdir -p target"
                // Safely unstash only if the stash was created
                for (String browser : browsersToTest) {
                    try {
                        unstash(name: "allure-results-${browser}")
                    } catch (Exception e) {
                        echo "No Allure results stash for ${browser}: " + e.toString()
                    }
                }
                sh "mkdir -p target/allure-results"
                sh "find target/allure-results-* -type f -exec cp {} target/allure-results/ \\; || true"
                echo "🧪 DEBUG: Listing merged Allure results"
                sh "ls -l target/allure-results || true"
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
