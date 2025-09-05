pipeline {
    agent any
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        durabilityHint('PERFORMANCE_OPTIMIZED')
        timeout(time: 30, unit: 'MINUTES')
    }
    
    environment {
        MAVEN_ARGS = '-B -ntp -q'
        TOMCAT_HOME = '/opt/tomcat'
        TOMCAT_WEBAPPS = "${TOMCAT_HOME}/webapps"
        TOMCAT_USER = 'tomcat'
        APP_NAME = 'NumberGuessGame'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo '🔄 Checking out source code...'
                checkout scm
                
                script {
                    echo "Build Number: ${env.BUILD_NUMBER}"
                    echo "Branch: ${env.BRANCH_NAME ?: 'main'}"
                    echo "Workspace: ${env.WORKSPACE}"
                }
            }
        }
        
        stage('Environment Check') {
            steps {
                echo '🔍 Checking build environment...'
                sh '''
                    echo "Java Version:"
                    java -version
                    echo "Maven Version:"
                    mvn --version
                    echo "Available disk space:"
                    df -h ${WORKSPACE}
                '''
            }
        }
        
        stage('Build & Test') {
            steps {
                echo '🏗️ Building and testing application...'
                script {
                    try {
                        // Clean and validate
                        sh "mvn ${env.MAVEN_ARGS} clean validate"
                        
                        // Compile
                        sh "mvn ${env.MAVEN_ARGS} compile"
                        
                        // Run tests
                        sh "mvn ${env.MAVEN_ARGS} test"
                        
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error("Build or test failed: ${e.getMessage()}")
                    }
                }
            }
            post {
                always {
                    // Publish test results
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    
                    // Publish test coverage if available
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'Code Coverage Report'
                    ])
                }
            }
        }
        
        stage('Package WAR') {
            steps {
                echo '📦 Packaging WAR file...'
                sh "mvn ${env.MAVEN_ARGS} package -DskipTests"
                
                script {
                    if (!fileExists("target/${APP_NAME}.war")) {
                        error("WAR file was not created successfully")
                    }
                    
                    def warSize = sh(script: "du -h target/${APP_NAME}.war | cut -f1", returnStdout: true).trim()
                    echo "WAR file size: ${warSize}"
                }
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo '📚 Archiving build artifacts...'
                archiveArtifacts artifacts: 'target/*.war', fingerprint: true, allowEmptyArchive: false
                archiveArtifacts artifacts: 'pom.xml', fingerprint: true, allowEmptyArchive: true
            }
        }
        
        stage('Pre-deployment Checks') {
            steps {
                echo '🔍 Running pre-deployment checks...'
                script {
                    // Check if Tomcat directory exists
                    def tomcatExists = sh(script: "test -d ${TOMCAT_HOME}", returnStatus: true) == 0
                    if (!tomcatExists) {
                        error("Tomcat directory ${TOMCAT_HOME} does not exist")
                    }
                    
                    // Check if we have write permissions
                    def canWrite = sh(script: "test -w ${TOMCAT_WEBAPPS}", returnStatus: true) == 0
                    if (!canWrite) {
                        echo "⚠️ Warning: May not have write permissions to ${TOMCAT_WEBAPPS}"
                    }
                    
                    echo "✅ Pre-deployment checks passed"
                }
            }
        }
        
        stage('Deploy to Tomcat') {
            steps {
                echo '🚀 Deploying to Tomcat...'
                script {
                    try {
                        // Backup existing deployment if it exists
                        sh """
                            if [ -f ${TOMCAT_WEBAPPS}/${APP_NAME}.war ]; then
                                echo '📁 Backing up existing deployment...'
                                cp ${TOMCAT_WEBAPPS}/${APP_NAME}.war ${TOMCAT_WEBAPPS}/${APP_NAME}.war.backup.\$(date +%Y%m%d_%H%M%S)
                            fi
                        """
                        
                        // Stop Tomcat gracefully
                        sh """
                            echo '🛑 Stopping Tomcat...'
                            if pgrep -f tomcat; then
                                ${TOMCAT_HOME}/bin/shutdown.sh
                                
                                # Wait up to 30 seconds for graceful shutdown
                                for i in {1..30}; do
                                    if ! pgrep -f tomcat > /dev/null; then
                                        echo 'Tomcat stopped gracefully'
                                        break
                                    fi
                                    echo "Waiting for Tomcat to stop... (\$i/30)"
                                    sleep 1
                                done
                                
                                # Force kill if still running
                                if pgrep -f tomcat > /dev/null; then
                                    echo '⚠️ Force killing Tomcat processes...'
                                    pkill -9 -f tomcat || true
                                    sleep 2
                                fi
                            else
                                echo 'Tomcat is not running'
                            fi
                        """
                        
                        // Remove old application directory
                        sh """
                            echo '🗑️ Cleaning old deployment...'
                            rm -rf ${TOMCAT_WEBAPPS}/${APP_NAME}
                            rm -f ${TOMCAT_WEBAPPS}/${APP_NAME}.war
                        """
                        
                        // Deploy new WAR
                        sh """
                            echo '📋 Deploying new WAR file...'
                            cp target/${APP_NAME}.war ${TOMCAT_WEBAPPS}/${APP_NAME}.war
                            
                            # Set proper permissions
                            chown ${TOMCAT_USER}:${TOMCAT_USER} ${TOMCAT_WEBAPPS}/${APP_NAME}.war 2>/dev/null || true
                            chmod 644 ${TOMCAT_WEBAPPS}/${APP_NAME}.war
                        """
                        
                        // Start Tomcat
                        sh """
                            echo '▶️ Starting Tomcat...'
                            ${TOMCAT_HOME}/bin/startup.sh
                            
                            echo '⏳ Waiting for Tomcat to start...'
                            for i in {1..60}; do
                                if curl -f -s http://localhost:8080/ > /dev/null 2>&1; then
                                    echo '✅ Tomcat is responding'
                                    break
                                fi
                                echo "Waiting for Tomcat to respond... (\$i/60)"
                                sleep 2
                            done
                        """
                        
                        // Test deployment
                        sh """
                            echo '🧪 Testing deployment...'
                            sleep 5
                            
                            # Check if application is accessible
                            for i in {1..30}; do
                                if curl -f -s http://localhost:8080/${APP_NAME}/ > /dev/null 2>&1; then
                                    echo '✅ Application is responding'
                                    break
                                elif [ \$i -eq 30 ]; then
                                    echo '❌ Application failed to respond after 60 seconds'
                                    exit 1
                                fi
                                echo "Waiting for application to respond... (\$i/30)"
                                sleep 2
                            done
                        """
                        
                        echo '✅ Deployment completed successfully!'
                        
                    } catch (Exception e) {
                        currentBuild.result = 'FAILURE'
                        error("Deployment failed: ${e.getMessage()}")
                    }
                }
            }
        }
        
        stage('Post-deployment Tests') {
            steps {
                echo '🧪 Running post-deployment smoke tests...'
                script {
                    try {
                        // Basic connectivity test
                        sh """
                            echo 'Testing application endpoints...'
                            
                            # Test main page
                            curl -f -s -o /dev/null http://localhost:8080/${APP_NAME}/guess || {
                                echo '❌ Main page not accessible'
                                exit 1
                            }
                            
                            echo '✅ Application endpoints are working'
                        """
                    } catch (Exception e) {
                        echo "⚠️ Post-deployment tests failed: ${e.getMessage()}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo '🎉 Build & Deployment successful!'
            script {
                def appUrl = "http://localhost:8080/${APP_NAME}/guess"
                echo "🌐 Application is available at: ${appUrl}"
            }
        }
        
        failure {
            echo '❌ Build or Deployment failed. Check logs.'
        }
        
        unstable {
            echo '⚠️ Build completed but tests failed or were unstable'
        }
        
        always {
            echo '🧹 Cleaning up workspace...'
            
            cleanWs(
                deleteDirs: true, 
                notFailBuild: true,
                patterns: [
                    [pattern: '.git', type: 'EXCLUDE'],
                    [pattern: 'target/surefire-reports', type: 'EXCLUDE']
                ]
            )
            
            script {
                echo """
                📊 Build Summary:
                - Job: ${env.JOB_NAME}
                - Build: #${env.BUILD_NUMBER}
                - Duration: ${currentBuild.durationString}
                - Result: ${currentBuild.result ?: 'SUCCESS'}
                """
            }
        }
    }
}
