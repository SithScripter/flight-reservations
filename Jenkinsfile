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

        // ✅ FIX: Uses the safest shell command to clean only the Allure plugin's history
        stage('Prepare Workspace') {
            steps {
                echo "🧹 Cleaning up old Allure report archives from previous builds..."
                sh 'rm -rf allure-report || true'
            }
        }

        stage('Build & Push') {
            steps {
                script {
                    echo "🧹 Forcefully cleaning old build artifacts..."
                    sh 'rm -rf target'

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

        stage('Clean Results Directory') {
            steps {
                echo "🧹 Cleaning up old Allure results directory..."
                sh 'rm -rf target/allure-results target/allure-results-* || true'
                sh 'mkdir -p target/allure-results'
            }
        }

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
                // This logic now correctly handles both single and cross-browser runs
                if (params.RUN_CROSS_BROWSER) {
                    echo "🧹 Cleaning final Allure results directory for merge..."
                    sh 'rm -rf target/allure-results || true'
                    sh 'mkdir -p target/allure-results'

                    echo "🤝 Merging Allure results from parallel runs..."
                    sh 'cp -r target/allure-results-*/. ./target/allure-results/ 2>/dev/null || true'

                    echo "📝 Consolidating environment properties from parallel runs..."
                    // ✅ FIX: This definitive script correctly preserves the full browser name and version
                    sh '''
                    # Start with a clean slate
                    rm -f target/allure-results/environment.properties || true
                    
                    # Copy common properties (everything EXCEPT Browser lines) from just one of the files
                    if [ -f target/allure-results-chrome/environment.properties ]; then
                        sed '/^Browser./d' target/allure-results-chrome/environment.properties >> target/allure-results/environment.properties
                    elif [ -f target/allure-results-firefox/environment.properties ]; then
                        sed '/^Browser./d' target/allure-results-firefox/environment.properties >> target/allure-results/environment.properties
                    fi
                    
                    # Collect ALL Browser.* lines from ALL files, extract the full value after '=', remove duplicates, and renumber
                    grep "^Browser." target/allure-results-*/environment.properties 2>/dev/null | cut -d'=' -f2- | sort -u | \
                    awk 'BEGIN {count=1} {print "Browser." count "=" $0; count++}' >> target/allure-results/environment.properties
                '''
                }

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