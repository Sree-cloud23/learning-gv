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
  def AgentTemplate = getAgentTemplateSD(config)
  def TenantName = "${params.TenantName}"
  def InputFile = "${params.InputFile}"
  def OutputFile = "${params.OutputFile}"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def Directory = "${params.Directory}"


  print "GitRepo=${GitRepo} GtiBranch=${GtiBranch}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        //sh "pwd;ls -lrht"
      }
    container(name: 'python') {
        sh """
          ls -la /cache/upload/SD/
          python upload_to_s3_v1.py ${TenantName} ${InputFile} ${OutputFile} ${Directory}

        """
      }
    }
  }
}
