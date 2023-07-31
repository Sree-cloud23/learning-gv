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
  def Tenant_SecretName = "${params.Tenant_SecretName}"
  def TenantDBName = "${params.tenantDBName}"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def Dburl = "${params.dbUrl}"
  def Clustname = "'${params.clustername}'"
  def TenantName = "'${params.tenantName}'"
  def Towername = "'${params.towername}'"

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
            export PGPASSWORD=${pw}
            psql -h '''+Dburl+''' -U ${user} -p 5432 -d postgres --variable=tenantDBName='''+TenantDBName.toLowerCase()+''' -f  CREATE_TenantDB/CREATE_IntellimapTenantDB.sql
            psql -h '''+Dburl+''' -U ${user} -p 5432 -d '''+TenantDBName.toLowerCase()+''' -f CREATE_SCHEMA_TenantDB/CREATE_SCHEMA_IntellimapTenantDB.sql
            psql -h '''+Dburl+''' -U ${user} -p 5432 -d '''+TenantDBName.toLowerCase()+''' --echo-all --variable=clustername='''+Clustname+''' --variable=towername='''+Towername+''' -f INSERT_Configurations_and_Master_Data_Seeding/Configurations_and_Master_Data_Seeding.sql
            psql -h '''+Dburl+''' -U ${user} -p 5432 -d '''+TenantDBName.toLowerCase()+''' --echo-all --variable=clustername='''+Clustname+''' --variable=towername='''+Towername+''' -f INSERT_Configurations_and_Master_Data_Seeding/Cluster_Tower_masterdata.sql
            psql -h '''+Dburl+''' -U ${user} -p 5432 -d "CommonConfigDB" --echo-all --variable=tenantName='''+TenantName+''' --variable=Tenant_SecretName='''+Tenant_SecretName+''' -f CommonConfigDB_DB_ENTRY/CommonConfig_Entry_SQL_Script.sql
          '''
          }
        }
      }
    }
  }
}