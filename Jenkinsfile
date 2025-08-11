def browsersToTest = []
def browserCount = 0

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
        ALLURE_CMD = tool 'Allure'
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    if (params.RUN_CROSS_BROWSER) {
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        def selectedBrowser = params.BROWSER?.toLowerCase()
                        browsersToTest = (selectedBrowser in ['chrome', 'firefox']) ? [selectedBrowser] : ['chrome']
                    }
                    browsersToTest = browsersToTest.unique()
                    browserCount = browsersToTest.size()
                    echo "✅ Browsers to test: ${browsersToTest.join(', ')}"
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
                                    sh "mkdir -p target/allure-results-${browser}/"
                                    sh "docker cp ${projectName}-tests:/home/flight-reservations/target/allure-results/. target/allure-results-${browser}/ || true"

                                    def resultCount = sh(script: "ls target/allure-results-${browser}/ 2>/dev/null | wc -l", returnStdout: true).trim()
                                    if (resultCount != "0") {
                                        stash name: "allure-results-${browser}", includes: "target/allure-results-${browser}/**"
                                    } else {
                                        echo "⚠️ No Allure results found for ${browser}, skipping stash."
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
                echo "🤝 Aggregating Allure results..."

                def finalResultsDir = 'allure-final-results'
                sh "rm -rf ${finalResultsDir} && mkdir -p ${finalResultsDir}"

                for (String browser : browsersToTest) {
                    try {
                        unstash name: "allure-results-${browser}"
                        sh "cp -r target/allure-results-${browser}/* ${finalResultsDir}/ || true"
                    } catch (e) {
                        echo "⚠️ No results for ${browser}: ${e}"
                    }
                }

                // --- Merge environment.properties cleanly ---
                sh """
                echo "Merging environment.properties from each browser..."
                TMP_ENV=\\$(mktemp)
                > "\\\$TMP_ENV"

                for dir in target/allure-results-*; do
                    if [ -f "\\\$dir/environment.properties" ]; then
                        browserName=\\$(basename "\\\$dir" | sed 's/^allure-results-//')

                        if [ ${browserCount} -gt 1 ]; then
                            awk -v prefix="\\\$browserName" '{
                                split(\\\$0,a,"=");
                                if (a[1] ~ /^Browser/ || a[1] ~ /^Browser.Version/) {
                                    print a[1] "." prefix "=" a[2];
                                } else {
                                    print \\\$0;
                                }
                            }' "\\\$dir/environment.properties" >> "\\\$TMP_ENV"
                        else
                            cat "\\\$dir/environment.properties" >> "\\\$TMP_ENV"
                        fi
                    fi
                done
                
                echo "Thread.Count=${params.THREAD_COUNT}" >> "\\\$TMP_ENV"
                echo "Test.Suite=${params.TEST_SUITE}" >> "\\\$TMP_ENV"

                mv "\\\$TMP_ENV" "${finalResultsDir}/environment.properties"
                echo "Merged environment.properties:"
                cat "${finalResultsDir}/environment.properties"
                """

                echo "🧪 Generating Allure Report via CLI..."
                sh """
                    rm -rf allure-report
                    ${ALLURE_CMD}/bin/allure generate ${finalResultsDir} -o allure-report --clean
                """

                allure includeProperties: false, jdk: '', report: 'allure-report'
            }
        }
    }
}