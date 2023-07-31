// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants
String getRegionAws(Map config = [:]) {

  def input = "${params.Region}" + '-' + "${params.Envir}"
  def result = ''
  switch (input) {
      case 'EU-QA':
          result = 'eu-central-1'
          break
      case 'EU-PROD':
          result = 'eu-central-1'
          break
      case 'NA-DEV':
          result = 'us-east-1'
          break
      case 'NA-QA':
          result = 'us-east-1'
          break
      case 'NA-PROD':
          result = 'us-east-1'
          break
  }
  return result
}

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = config.GitRepo
  def GitBranch = "${params.branch}"
  def gitCommit
  def ReleaseDir = "deployment"
  def AgentTemplate = getAgentTemplateEaf(config)
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def InstanceID = "${params.InstanceID}"

  print "GitRepo=${GitRepo} GitBranch=${GitBranch}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GitBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
      }
    stage('VM script') {
      container(name: 'utils') {
        withCredentials([usernamePassword(credentialsId: 'git-devops', passwordVariable: 'password', usernameVariable: 'userName')]) {
          def gituser = "${userName}"
          def gitpass = "${password}"
          sh '''
            sh_command_id=$(aws ssm send-command \
                --instance-ids "'''+InstanceID+'''" \
                --document-name "AWS-RunShellScript" \
                --region "'''+getRegionAws(config)+'''" \
                --comment "shell script on Linux Instances" \
                --parameters '{"commands":["cd /tmp && git clone -b VM_ml https://'''+gituser+''':'''+gitpass+'''@github.com/capgemini-gadm/EAF_DevOps.git && cd /tmp/EAF_DevOps/ && sudo sh -x Ikon_ml.sh '''+GitBranch+''' '''+Region+''' '''+Envir+''' '''+gituser+''' '''+gitpass+''' && rm -fr /tmp/EAF_DevOps"]}' \
                --output text \
                --query "Command.CommandId") sh -c 'aws ssm list-command-invocations \
                --command-id "$sh_command_id" \
                --details \
                --query "CommandInvocations[].CommandPlugins[].{Status:Status,Output:Output}"'
            '''
          }
        }
      }
    }
  }
}