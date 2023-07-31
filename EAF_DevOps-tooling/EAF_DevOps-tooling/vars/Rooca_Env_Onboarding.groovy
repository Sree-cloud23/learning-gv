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
  def TenanDBName = "${params.tenantDBName}"
  def TenantDir = "${params.tenantDir}"
  def Dburl = "${params.dbUrl}"
  def LookDBName = "${params.lookDBName}"
  def UserName = "${params.UserName}"

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
      container(name: 'postgres') {
        withCredentials([usernamePassword(credentialsId: "${params.cred}", passwordVariable: 'pw',  usernameVariable: 'user')]) {
          sh '''
                #!/bin/sh
                export PGPASSWORD=${pw}
                psql -h '''+Dburl+''' -U ${user} -p 5432 -d postgres --variable=userName='''+UserName+''' --variable=password='''+Password+'''  -f CREATE_LookUPDB/CREATE_DB_LookupDB.sql
                export PGPASSWORD="${Password}"
                psql -h '''+Dburl+''' -U ${UserName} -p 5432 -d Common -f CREATE_SCHEMA_LookUPDB/CREATE_SCHEMA_LookUPDB.sql
                
            '''
          }
        }
      }
    }
  }
}
