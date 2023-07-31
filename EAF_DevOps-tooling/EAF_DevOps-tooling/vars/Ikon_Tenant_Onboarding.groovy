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
  def Dburl = "${params.dbUrl}"
  def Password = "${params.Password}"
  def AccountName = "${params.AccountName}"
  def TenantName = "${params.tenantName}"
  def StaticToken = "${params.StaticToken}"
  def KoPrefix = "${params.KoPrefix}"
  def KoStartingNo = "${params.KoStartingNo}"
  def LookupDB = "${params.LookupDB}"
  def UserName = "${params.UserName}"
  def DlKoLinkageApi = "${params.DlKoLinkageApi}"
  def LookupDBPass = "${params.LookupDBPass}"
  def LookupDBUserName = "${params.LookupDBUserName}"
  def LookupDBName = "${params.LookupDBName}"
  def LookupDBSchemaName = "${params.LookupDBSchemaName}"

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
               export PGPASSWORD='''+LookupDBPass+'''
               psql -h '''+Dburl+''' -U '''+LookupDBUserName+''' -p 5432 -d '''+LookupDBName+''' -v tenantName="'''+TenantName+'''" -v accountName="'''+AccountName+'''" -v tenantDBName='''+TenantDBName+''' -v dbUrl='''+Dburl+''' -v secretName='''+Secret_Name+''' -v koPrefix='''+KoPrefix+''' -v koStartingNo='''+KoStartingNo+''' -v staticToken='''+StaticToken+''' -v dlKoLinkageApi='''+DlKoLinkageApi+''' -v lookupdbschemaname='''+LookupDBSchemaName+''' -f LOOKUP_DB_ENTRY/LOOKUP_DB_ENTRY.sql
               export PGPASSWORD=${Password}
               psql -h '''+Dburl+''' -U '''+UserName+''' -p 5432 -d '''+TenantDBName+''' -v accountName="'''+AccountName+'''" -f CREATE_SCHEMA_TenantDB/CREATE_SCHEMA_TenantDB.sql
            '''
          }
        }
      }
    }
  }
}
