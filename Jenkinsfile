node {

    stage("Checkout") {
        checkout scm
    }

    stage("Publishing") {
        def gradleCommand = "./gradlew --info --stacktrace "
        def gitBranch = env.BRANCH_NAME
        if(gitBranch.startsWith("release") || gitBranch == "master") {
            withCredentials([
                    usernamePassword(credentialsId: 'sonatype', passwordVariable: 'sonatypePassword', usernameVariable: 'sonatypeUsername'),
                    string(credentialsId: 'gpgpassphrase', variable: 'gpgPassPhrase')]) {
                sh "${gradleCommand} publish " +
                        " -Ppublish_username=${sonatypeUsername} " +
                        " -Ppublish_password=${sonatypePassword} " +
                        " -PkeyId=DF8285F0 " +
                        " -Ppassword=${gpgPassPhrase} " +
                        " -PsecretKeyRingFile=/var/jenkins_home/secring.gpg "
            }
        }
    }
}
