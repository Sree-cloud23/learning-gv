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
  def Secret_UserName = "${params.Secret_UserName}"
  def TenantDBName = "${params.tenantDBName}"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def Dburl = "${params.dbUrl}"
  def Secret_PasswordName = "${params.Secret_PasswordName}"
  def Status = "${params.status}"
  def TenantName = "'${params.tenantName}'"
  def TenantID = "${params.tenantID}"
  def TenantType = "${params.tenantType}"

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
                psql -h '''+Dburl+''' -U ${user} -p 5432 --variable=tenantDBName='''+TenantDBName+''' -d postgres -f  CREATE_TenantDB/CREATE_DATABASE.sql
                for entry in CREATE_SCHEMA_TenantDB/*sql DATA_SEEDING_From_CSV/*sql Configurations_and_Miscellaneous_Data_Seeding/*sql 
                  do
                   if [ -f "$entry" ]; then
                    psql -h '''+Dburl+''' -U ${user} -p 5432 -f $entry -d '''+TenantDBName+'''
                   
                   fi
                  done
                psql -h '''+Dburl+''' -U ${user} -p 5432 -d LookupDB --echo-all --variable=dbUrl='''+Dburl+''' --variable=tenantDBName='''+TenantDBName+''' --variable=Secret_UserName='''+Secret_UserName+''' --variable=Secret_PasswordName='''+Secret_PasswordName+''' --variable=tenantName='''+TenantName+''' --variable=tenantID='''+TenantID+''' --variable=status='''+Status+''' --variable=tenantType='''+TenantType+''' -f  LOOKUP_DB_ENTRY/LookupDB_Entry_SQL_Script.sql
            '''
          }
        }
      }
    }
  }
}
