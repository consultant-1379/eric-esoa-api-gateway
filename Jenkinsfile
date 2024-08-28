def bob = new BobCommand()
    .envVars([
        SELI_ARTIFACTORY_REPO_USER:'${CREDENTIALS_SELI_ARTIFACTORY_USR}',
        SELI_ARTIFACTORY_REPO_PASS:'${CREDENTIALS_SELI_ARTIFACTORY_PSW}',
        SONAR_ENTERPRISE_AUTH_TOKEN:'ee9b1b7cbd0a5e223c3d8928bd78b48f6c9f6403'
    ]).toString()

pipeline {
    agent {
        node {
            label params.SLAVE
        }
    }

    parameters {
        string(name: 'SETTINGS_CONFIG_FILE_NAME', defaultValue: 'maven.settings.eso')
    }

    environment {
        CHART_REPO = 'https://arm.rnd.ki.sw.ericsson.se/artifactory/proj-so-gs-all-helm'
        CHART_NAME = 'eric-esoa-api-gateway'
        CREDENTIALS_SELI_ARTIFACTORY = credentials('eoadm100_user_creds')
        SELI_ARTIFACTORY_REPO_PASS = '${CREDENTIALS_SELI_ARTIFACTORY_PSW}'
        SELI_ARTIFACTORY_REPO_USER = '${CREDENTIALS_SELI_ARTIFACTORY_USR}'
        DOCKER_CONFIG_JSON_FILE_NAME = 'armdockerconfig'
        SONAR_ENTERPRISE_AUTH_TOKEN = credentials('SONAR_ENTERPRISE_AUTH_TOKEN')
    }

    stages {
        stage('Inject Settings.xml File') {
            steps {
                configFileProvider([configFile(fileId: "${env.SETTINGS_CONFIG_FILE_NAME}", targetLocation: "${env.WORKSPACE}")]) {
                }
                configFileProvider([configFile(fileId: "${env.DOCKER_CONFIG_JSON_FILE_NAME}", targetLocation: "${env.WORKSPACE}/.docker/config.json")]) {
                }
            }
        }

        stage('Clean') {
            steps {
                sh "${bob} clean"
                sh 'git clean -xdff --exclude=.m2 --exclude=.sonar --exclude=settings.xml --exclude=.docker --exclude=.kube'
            }
        }

        stage('Init') {
            steps {
                sh "${bob} init"
                archiveArtifacts 'artifact.properties'
            }
        }

        stage('Chart dependency update') {
            steps {
                sh "${bob} chart:dependency-update"
            }
        }

        stage('Lint') {
            steps {
                sh "${bob} lint"
            }
            post {
                always {
                    archiveArtifacts allowEmptyArchive: true, artifacts: '**/*bth-linter-output.html, .bob/design-rule-check-report.*'
                }
            }
        }

        stage('Generate') {
            steps {
                sh "${bob} rest-2-html:check-has-open-api-been-modified"
                script {
                    def val = readFile '.bob/var.has-openapi-spec-been-modified'
                    if (val.trim().equals("true")) {
                        sh "${bob} rest-2-html:zip-open-api-doc"
                        sh "${bob} rest-2-html:generate-html-output-files"
                        if(params.RELEASE == "true"){
                           addInfoBadge "OpenAPI spec has changed. HTML Output files will be published to the CPI library."
                           archiveArtifacts 'rest_conversion_log.txt'
                        }
                        if(params.RELEASE == "false"){
                           addInfoBadge "OpenAPI spec has changed. Review the Archived HTML Output files: rest2html*.zip and"
                           archiveArtifacts 'rest_conversion_log.txt, rest2html*.zip'
                        }
                    }
                }
            }
        }

        stage('Build package and execute tests') {
            steps {
                sh "chmod +x ./automatic-image-version-update.sh"
                sh "./automatic-image-version-update.sh"
                sh "${bob} build"
            }
        }
        stage('ADP Helm Design Rule Check') {
            steps {
                sh "${bob} adp-helm-dr-check"
                archiveArtifacts 'design-rule-check-report.*'
            }
        }
        stage('Build image and chart') {
            steps {
                sh "${bob} image"
            }
        }

        stage('SonarQube full analysis') {
            steps {
                sh "${bob} sonar"
            }
        }

        stage('Push image, chart & tag commit (Using bob)') {
            when {
                expression { params.RELEASE == "true" }
            }
            steps {
                sh "${bob} package"
                echo "bob package was successful"
                sh "${bob} publish"
                echo "bob publish was successful"
                sh "${bob} tagging"
                echo "bob tagging was successful"
                script {
                    def props = readProperties file: 'artifact.properties'
                    addInfoBadge props['CHART_NAME'] + "-" + props['CHART_VERSION']
                }
            }
        }
    }
    post {
        always {
            archive "**/target/surefire-reports/*"
            junit '**/target/surefire-reports/*.xml'
            step([$class: 'JacocoPublisher'])
        }
    }
}


// More about @Builder: http://mrhaki.blogspot.com/2014/05/groovy-goodness-use-builder-ast.html
import groovy.transform.builder.Builder
import groovy.transform.builder.SimpleStrategy

@Builder(builderStrategy = SimpleStrategy, prefix = '')
class BobCommand {
    def bobImage = 'armdocker.rnd.ericsson.se/sandbox/adp-staging/adp-cicd/bob.2.0:1.7.0-20'
    def envVars = [:]
    def additionalVolumes = []
    def needDockerSocket = true

    // executes bob with --quiet parameter to reduce noise. Set to false if you need to get more details.
    def quiet = true

    String toString() {
        def env = envVars
                .collect({ entry -> "-e ${entry.key}=\"${entry.value}\"" })
                .join(' ')
        def volumes = additionalVolumes
                .collect({ line -> "-v \"${line}\"" })
                .join(' ')
        def cmd = """\
            |docker run
            |--init
            |--rm
            |--workdir \${PWD}
            |--user \$(set +x; id -u):\$(set +x; id -g)
            |-v \${PWD}:\${PWD}
            |-v \${PWD}/.docker:\${HOME}/.docker
            |-v /etc/group:/etc/group:ro
            |-v /etc/passwd:/etc/passwd:ro
            |-v \${HOME}:\${HOME}
            |-v \${HOME}/.ssh:\${HOME}/.ssh
            |${needDockerSocket ? '-v /var/run/docker.sock:/var/run/docker.sock' : ''}
            |${env}
            |${volumes}
            |\$(set +x; for group in \$(id -G); do printf ' --group-add %s' "\$group"; done)
            |${bobImage}
            |${quiet ? '--quiet' : ''}
            |"""
        return cmd
                .stripMargin()           // remove indentation
                .replace('\n', ' ')      // join lines
                .replaceAll(/[ ]+/, ' ') // replace multiple spaces by one
    }
}