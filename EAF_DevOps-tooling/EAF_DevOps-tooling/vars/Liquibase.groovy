// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = config.GitRepo
  def GtiBranch = "${params.branch}"
  def gitCommit
  def AppName = config.AppName
  def BUILD_NUMBER = env.BUILD_NUMBER
  def ImageName = config.ImageName
  def ReleaseDir = "deployment"
  def AgentTemplate = getAgentTemplate(config)
  def TenantDBName = "${params.TenantDBName}"
  def SchemaName = "${params.SchemaName}"
  def SourceDburl = "${params.SourceDburl}"
  def Proj = "EAF"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def AppEnv = Proj + "/" + Region + "/" + Envir
  def Year = new Date().format("yyyy")
  def DestDburl = "${params.DestDburl}"
  def RefDBName = "${params.RefDBName}"

  print "GitRepo=${GitRepo} GtiBranch=${GtiBranch}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
      }
    stage('DB script') {
      container(name: 'liquibase') {
        withCredentials([
                    usernamePassword(credentialsId: "${params.SourceDBCred}", usernameVariable: 'USER1', passwordVariable: 'PASS1'),
                    usernamePassword(credentialsId: "${params.DestDBCred}", usernameVariable: 'USER2', passwordVariable: 'PASS2')
                ]) {
          sh '''
                #!/bin/sh
                liquibase --changeLogFile=/tmp/db.changelogdiff_Q2_'''+SchemaName+'''.sql --outputFile=/tmp/db.changelogdiff_Q2.txt --driver=org.postgresql.Driver --classpath=/liquibase/postgresql-42.2.11.jar --url="jdbc:postgresql://'''+SourceDburl+''':5432/'''+TenantDBName+'''?currentSchema='''+SchemaName+'''" --username=${USER1} --password=${PASS1} diffChangeLog --referenceUrl="jdbc:postgresql://'''+DestDburl+''':5432/'''+RefDBName+'''?currentSchema='''+SchemaName+'''" --referenceUsername=${USER2} --referencePassword=${PASS2}

                cat /tmp/db.changelogdiff_Q2_'''+SchemaName+'''.sql
               
            '''
          }
        }
      }
    }
  }
}


