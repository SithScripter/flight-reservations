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
                if (params.RUN_CROSS_BROWSER) {
                    echo "🧹 Cleaning final Allure results directory for merge..."
                    sh 'rm -rf target/allure-results || true'
                    sh 'mkdir -p target/allure-results'

                    echo "🤝 Merging Allure test case results from parallel runs..."
                    sh 'cp -r target/allure-results-*/. ./target/allure-results/ 2>/dev/null || true'

                    echo "📝 Consolidating environment properties using Groovy..."
                    def commonProps = []
                    def browserProps = []

                    // Find all the generated environment files using findFiles
                    def envFiles = findFiles(glob: 'target/allure-results-*/environment.properties')
                    echo "Found environment files: ${envFiles.collect { it.path }.join(', ')}" // Corrected debug output

                    if (envFiles) {
                        // Read common properties from the first file
                        def firstFileContent = readFile(envFiles[0].path).trim()
                        commonProps = firstFileContent.readLines().findAll { !it.startsWith('Browser.') }

                        // Read all browser properties from all files
                        envFiles.each { file ->
                            def fileContent = readFile(file.path).trim()
                            browserProps.addAll(fileContent.readLines().findAll { it.startsWith('Browser.') })
                        }
                    }

                    // Remove duplicates and renumber the browser properties
                    def uniqueBrowserProps = browserProps.unique()
                    def finalBrowserProps = []
                    uniqueBrowserProps.eachWithIndex { prop, i ->
                        def value = prop.split('=', 2)[1]
                        finalBrowserProps.add("Browser.${i + 1}=${value}")
                    }

                    // Combine and write the final, clean properties file
                    def finalProps = (commonProps + finalBrowserProps).join('\n')
                    writeFile(file: 'target/allure-results/environment.properties', text: finalProps)

                    // Archive source files for inspection
                    archiveArtifacts artifacts: 'target/allure-results-*/environment.properties', allowEmptyArchive: true
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