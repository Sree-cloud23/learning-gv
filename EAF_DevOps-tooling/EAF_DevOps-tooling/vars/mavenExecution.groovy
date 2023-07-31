#!/usr/bin/groovy

def call(Map stageParams) {
   // def mvn = tool 'mvn3.6'
container('maven') {
   //withCredentials([usernamePassword(credentialsId: 'nexus-robot', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
    println("====${stageParams.buildParams}====")
    sh "mvn ${stageParams.buildParams}"
    // -s /usr/share/maven/ref/settings.xml"
    //-Dmaven.test.failure.ignore=true -DnexusUsername=${NEXUS_USERNAME} -DnexusPassword=${NEXUS_PASSWORD} -Dnexus_domain=${NEXUS_HOSTNAME}"
     //}
   }
}
