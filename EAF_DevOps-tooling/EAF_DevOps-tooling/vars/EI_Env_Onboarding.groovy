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
  def TenantName = "${params.tenantName}"
  def TenantID = "${params.tenantID}"
  def TenantType = "${params.tenantType}"
  def AppEnv = TenantName + "/" + "EAF" + "/" + Region + "/" + Envir

  
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
                psql -h '''+Dburl+''' -U ${user} -p 5432 -d '''+TenantDBName+''' -f  '''+AppEnv+'''/Miscellaneous_Data_Seeding/*sql
                psql -h '''+Dburl+''' -U ${user} -p 5432 -d '''+TenantDBName+''' -f  '''+AppEnv+'''/DATA_SEEDING_From_CSV/*sql
                            
                  
            '''
          }
        }
      }
    }
  }
}
