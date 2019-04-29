pipeline {
    agent {
        dockerfile {
            filename 'docker/dockerfile-java'
            additionalBuildArgs '--build-arg JENKINS_USER_ID=`id -u jenkins` --build-arg JENKINS_GROUP_ID=`id -g jenkins`'
        }
    }

    parameters {
        string(name: 'TAG', defaultValue: '1.0.0', description: 'Tag')
        gitParameter name: 'BRANCH', branchFilter: 'origin/(.*)', defaultValue: 'master', type: 'PT_BRANCH'
    }

    environment {
        S3_REPO_ID='maven-repo.opendatahub.bz.it'
        S3_REPO_USERNAME=credentials('s3_repo_username')
        S3_REPO_PASSWORD=credentials('s3_repo_password')
    }

    stages {
        stage('Configure') {
            steps {
                sh 'sed -i -e "s/<\\/settings>$//g\" ~/.m2/settings.xml'
                sh 'echo "    <servers>" >> ~/.m2/settings.xml'
                sh 'echo "        <server>" >> ~/.m2/settings.xml'
                sh 'echo "            <id>${S3_REPO_ID}</id>" >> ~/.m2/settings.xml'
                sh 'echo "            <username>${S3_REPO_USERNAME}</username>" >> ~/.m2/settings.xml'
                sh 'echo "            <password>${S3_REPO_PASSWORD}</password>" >> ~/.m2/settings.xml'
                sh 'echo "        </server>" >> ~/.m2/settings.xml'
                sh 'echo "    </servers>" >> ~/.m2/settings.xml'
                sh 'echo "</settings>" >> ~/.m2/settings.xml'

                sh 'xmlstarlet ed -P -L -N pom="http://maven.apache.org/POM/4.0.0" -u "/pom:project/pom:version" -v $TAG pom.xml'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn -B -U clean verify -P it'
            }
        }
        stage('Release') {
            steps {
		        sh 'mvn -B -U clean deploy'
            }
        }
        stage('Tag') {
            steps {
                sshagent (credentials: ['jenkins_github_ssh_key']) {
                    sh "git config --global user.email 'info@opendatahub.bz.it'"
                    sh "git config --global user.name 'Jenkins'"
                    sh "git commit -a -m 'Version ${params.TAG}' --allow-empty"
                    sh "git tag -d ${params.TAG} || true"
                    sh "git tag -a ${params.TAG} -m ${params.TAG}"
                    sh "mkdir -p ~/.ssh"
                    sh "ssh-keyscan -H github.com >> ~/.ssh/known_hosts"
                    sh "git push origin HEAD:${params.BRANCH} --follow-tags"
                }
            }
        }

    }
}
