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
                psql -h '''+Dburl+''' -U ${user} -p 5432 -d postgres -f  CREATE_wso2am_db/CREATE_DB_wso2am_db.sql
                psql -h '''+Dburl+''' -U ${user} -p 5432 -d postgres -f  CREATE_wso2shared_db/CREATE_DB_wso2shared_db.sql
                for entry in CREATE_SCHEMA_wso2am_db/*.sql
                  do
                   if [ -f "$entry" ]; then
                   psql -h '''+Dburl+''' -U ${user} -p 5432 -f $entry -d wso2am_db
                   
                   if [[ 0 -ne $? ]]; then
                   exit 1
                    fi
                   fi
                  done
                for entry in CREATE_SCHEMA_wso2shared_db/*.sql
                  do
                   if [ -f "$entry" ]; then
                   psql -h '''+Dburl+''' -U ${user} -p 5432 -f $entry -d wso2shared_db
                   
                   if [[ 0 -ne $? ]]; then
                   exit 1
                    fi
                   fi
                  done
            '''
          }
        }
      }
    }
  }
}
