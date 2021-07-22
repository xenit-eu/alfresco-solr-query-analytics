node {

    stage("Checkout") {
        checkout scm
    }

    try {
        stage("Publishing") {
            def gradleCommand = "./gradlew --info --stacktrace "
            def gitBranch = env.BRANCH_NAME
            if(gitBranch.startsWith("release") || gitBranch == "master") {
                def sonatypeCredentials = usernamePassword(
                        credentialsId: 'sonatype',
                        passwordVariable: 'sonatypePassword',
                        usernameVariable: 'sonatypeUsername'
                );
                def gpgCredentials = string(credentialsId: 'gpgpassphrase', variable: 'gpgPassPhrase');
                withCredentials(sonatypeCredentials, gpgCredentials) {
                    sh "${gradleCommand} publish " +
                            " -PbuildNumber=${buildNr} " +
                            " -Ppublish_username=${sonatypeUsername} " +
                            " -Ppublish_password=${sonatypePassword} " +
                            " -PkeyId=DF8285F0 " +
                            " -Ppassword=${gpgPassPhrase} " +
                            " -PsecretKeyRingFile=/var/jenkins_home/secring.gpg "
                }
            }
        }
    } catch (err) {
        echo "Exception: ${err.getMessage()}"
        currentBuild.result = "FAILED"
    }
}
