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
  def Languages = "${params.languages}"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def Model_Name = "${params.Model_Name}"
  def Version_nu = "${params.Version}"
  def Proj = "EAF"
  def AppEnv = Proj + "/" + Region + "/" + Envir


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
    stage('ConfigUpdateForIntelliMapAppml') {
        def exists = fileExists "${AppEnv}/EFS"
        echo "===${exists}==="
        if (exists) {
          sh "pwd"
          sh "cp -Rpv ${AppEnv}/EFS/* EFS/"
        }
      }
    container(name: 'python') {
        sh """
        python Training_Module/Estimator_jenkins.py ${Model_Name} ${Languages} ${Version_nu}
        
        """
      }
    }
  }
}