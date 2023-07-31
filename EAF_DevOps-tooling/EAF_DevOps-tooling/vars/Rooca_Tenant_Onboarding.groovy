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
  def Secret_Name = "${params.Secret_Name}"
  def TenantDBName = "${params.tenantDBName}"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def Dburl = "${params.Dburl}"
  def Password = "${params.Password}"
  def Accountid = "${params.accountid}"
  def TenantName = "${params.tenantName}"
  def TenantID = "${params.tenantID}"
  def TenantType = "${params.tenantType}"
  def UserName = "${params.userName}"
  def LookupDB = "${params.LookupDB}"
  def LookupDBPass = "${params.LookupDBPass}"
  def LookupDBUserName = "${params.LookupDBUserName}"


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
                psql -h '''+Dburl+''' -U ${user} -p 5432 -d postgres --variable=userName='''+UserName+''' --variable=password='''+Password+''' --variable=tenantDBName='''+TenantDBName+''' -f CREATE_TenantDB/CREATE_TenantDB.sql
                export PGPASSWORD="'''+LookupDBPass+'''"
                psql -h '''+Dburl+''' -U '''+LookupDBUserName+''' -p 5432 -d Common  -v tenantName="'''+TenantName+'''" -v accountid='''+Accountid+''' -v tenantDBName='''+TenantDBName+''' -v dbUrl='''+Dburl+''' -v secretName='''+Secret_Name+''' -f LOOKUP_DB_ENTRY/LOOKUP_DB_ENTRY.sql
                export PGPASSWORD="${Password}"
                psql -h '''+Dburl+''' -U '''+UserName+''' -p 5432 -d '''+TenantDBName+''' -f CREATE_SCHEMA_TenantDB/CREATE_SCHEMA_TenantDB.sql
            '''
          }
        }
      }
    }
  }
}