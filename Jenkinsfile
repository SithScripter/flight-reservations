// Jenkinsfile - Simplified, Plugin-Only Approach

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
                    browsersToTest = []
                    if (params.RUN_CROSS_BROWSER) {
                        browsersToTest = ['chrome', 'firefox']
                    } else {
                        def selectedBrowser = params.BROWSER?.toLowerCase()
                        browsersToTest = (selectedBrowser in ['chrome', 'firefox']) ? [selectedBrowser] : ['chrome']
                    }
                    browsersToTest = browsersToTest.unique()
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

                                    // ✅ Stash only if results exist
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

                def finalReportDir = 'allure-final-results'
                sh "rm -rf ${finalReportDir} && mkdir -p ${finalReportDir}"

                // Unstash + copy results
                for (String browser : browsersToTest) {
                    try {
                        unstash name: "allure-results-${browser}"
                        sh "cp -r target/allure-results-${browser}/* ${finalReportDir}/ || true"
                    } catch (Exception e) {
                        echo "⚠️ No results for ${browser}: ${e}"
                    }
                }

                // --- Merge environment.properties content from each browser ---
                sh """
                echo "Merging environment.properties from each browser..."
                TMP_ENV=\$(mktemp)
                > "\$TMP_ENV"
                for dir in target/allure-results-*; do
                    if [ -f "\$dir/environment.properties" ]; then
                        browserName=\$(basename "\$dir" | sed 's/^allure-results-//')
                        sed "s/^/[\\\${browserName}] /" "\$dir/environment.properties" >> "\$TMP_ENV"
                    fi
                done
                mv "\$TMP_ENV" "${finalReportDir}/environment.properties"
                echo "Merged environment.properties:"
                cat "${finalReportDir}/environment.properties"
                """

                echo "🧪 Generating Allure Report..."
                sh "ls -l ${finalReportDir} || true"
                allure(
                        results: [[path: finalReportDir]],
                        reportBuildPolicy: 'ALWAYS'
                )
            }
        }
    }
}
