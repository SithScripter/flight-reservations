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
        stage('Build & Push') {
            steps {
                script {
                    echo "📦 Building JAR and preparing resources..."
                    sh 'mvn clean package -DskipTests'

                    echo "🐳 Building Docker Image..."
                    //def app = docker.build("${env.IMAGE_NAME}:${env.BUILD_NUMBER}", ".")
                    def app = docker.build("${env.IMAGE_NAME}:${env.BUILD_NUMBER}", "--no-cache .")

                    echo "🔐 Logging in and Pushing Docker Images..."
                    docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-creds') {
                        app.push("latest")
                        app.push("${env.BUILD_NUMBER}")
                    }
                }
            }
        }
        //force the multibranch pipeline to run scan again
        stage('Clean Results Directory') {
            steps {
                echo "🧹 Cleaning up old Allure results from previous builds..."
                sh 'rm -rf target/allure-results* target/allure-results-chrome/ target/allure-results-firefox/ || true'
                sh 'mkdir -p target/allure-results'
            }
        }

        // This stage runs ONLY if the 'RUN_CROSS_BROWSER' box is checked
        stage('Run Cross-Browser Suite') {
            when {
                expression { params.RUN_CROSS_BROWSER == true }
            }
            matrix {
                axes {
                    axis {
                        name 'BROWSER'
                        values 'chrome', 'firefox'
                    }
                }
                stages {
                    stage('Test on ${BROWSER}') {
                        steps {
                           script {

                               def projectName = "tests_${BROWSER}_${env.BUILD_NUMBER}"
                               try {
                                   echo "🚀 Launching ${params.TEST_SUITE} on ${BROWSER}..."
                                   sh "COMPOSE_PROJECT_NAME=${projectName} ENV=${params.ENV} TEST_SUITE=${params.TEST_SUITE} BROWSER=${BROWSER} THREAD_COUNT=${params.THREAD_COUNT} docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations"
                               } catch (any) {
                                   error("Tests failed for suite ${params.TEST_SUITE} on ${BROWSER}.")
                               } finally {
                                   // ✅ FIX: Copy results to a separate, browser-specific folder to prevent race conditions
                                   echo "📂 Copying Allure results from ${BROWSER} container..."
                                   sh "mkdir -p ./target/allure-results-${BROWSER}/"
                                   sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. ./target/allure-results-${BROWSER}/ || true"

                                   echo "🧹 Tearing down ${BROWSER} test environment..."
                                   sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
                               }
                           }
                        }
                    }
                }
            }
        }

        // This stage runs ONLY if the 'RUN_CROSS_BROWSER' box is UNCHECKED
        stage('Run Single-Browser Suite') {
            when {
                expression { params.RUN_CROSS_BROWSER == false }
            }
            steps {
                script {

                    def projectName = "tests_single_${env.BUILD_NUMBER}"
                    try {
                        echo "🚀 Launching ${params.TEST_SUITE} on ${params.BROWSER}..."
                        sh "COMPOSE_PROJECT_NAME=${projectName} ENV=${params.ENV} TEST_SUITE=${params.TEST_SUITE} BROWSER=${params.BROWSER} THREAD_COUNT=${params.THREAD_COUNT} docker-compose -f docker-compose.test.yml up --exit-code-from flight-reservations"
                    } catch (any) {
                        error("Tests failed for suite ${params.TEST_SUITE} on ${params.BROWSER}.")
                    } finally {
                        echo "📂 Copying Allure results from container..."
                        sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. ./target/allure-results/ || true"

                        echo "🧹 Tearing down test environment..."
                        sh "COMPOSE_PROJECT_NAME=${projectName} docker-compose -f docker-compose.test.yml down -v || true"
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                // ✅ FIX: Add this block to merge results before generating the report
                echo "🤝 Merging Allure results from all parallel runs..."
                //sh 'cp -r target/allure-results-*/. ./target/allure-results/ 2>/dev/null || true'
                sh 'cp -r target/allure-results-chrome/. target/allure-results/ 2>/dev/null || true'
                sh 'cp -r target/allure-results-firefox/. target/allure-results/ 2>/dev/null || true'

                echo "📋 Debugging Allure results in chrome and firefox folders..."
                sh 'ls -l target/allure-results-chrome/ || echo "No chrome results"'
                sh 'ls -l target/allure-results-firefox/ || echo "No firefox results"'

                echo "🧪 Generating Allure Report..."
                if (fileExists('target/allure-results') && sh(script: 'ls -A target/allure-results | wc -l', returnStdout: true).trim() != '0') {
                    allure(results: [[path: 'target/allure-results']])
                } else {
                    echo "⚠️ No Allure results found — skipping report generation."
                }

                echo "🧹 Cleaning up workspace..."
                cleanWs()

                echo "✅ Pipeline completed."
            }
        }
    }
}