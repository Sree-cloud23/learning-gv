// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = config.GitRepo
  def GitRepoPlf = config.GitRepoPlf
  def GtiBranchPlf = config.GtiBranchPlf
  def GtiBranch = "${params.branch}"
  def Registry_Cred = config.Registry_Cred
  def ImageName = config.ImageName
  def gitCommit
  def AppName = config.AppName
  def Region = "${params.Region}"
  def Proj = config.Proj
  def Envir = config.Envir
  def HostnameOverride = config.HostnameOverride
  def ArtifactTag = "${params.ArtifactTag}"
  //def AZURE_DEVOPS_EXT_PAT = "${env.AZURE_PAT}"
  def AgentTemplate = getAgentTemplate(config)


    print "GitRepo=${GitRepo} GtiBranch=${GtiBranch} GitRepoPlf=${GitRepoPlf} GtiBranchPlf=${GtiBranchPlf} Region=${Region}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
   stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        sh "pwd && ls"
      }
   
      stage('ConfigUpdate') {
        dir('platform') {
              gitCheckout(
                branch: "${GtiBranchPlf}", url: "https://github.com/capgemini-gadm/${GitRepoPlf}", credentialsId: "git-devops")
        }
        sh "pwd && ls platform/"
        def exists = fileExists "platform/${AppName}/${Proj}/${Region}/${Envir}/"
        echo "===${exists}==="
        if (exists) {
          sh "cp -Rpv platform/${AppName}/${Proj}/${Region}/${Envir}/* src/main/resources/"
          sh "cat src/main/resources/application.properties"
        }
      }
   
    stage('MavenBuild') {
      println("maven install")
      mavenExecution(
        buildParams : "clean install -Dmaven.test.skip=true"
        )
      }
   
       // START: Docker tag random id
       def verCode = UUID.randomUUID().toString()
       def Dtag = verCode.substring(0,8)
       println(Dtag)
       def gitCommit2 = gitCommit+Dtag
      // END: Docker tag random id 
   
      stage('Docker Build') {
      printTime("docker build image")
      def registryHostname =  "${env.BASE_REGISTRY}"
      def projectname = "capgemini-gadm"
      def componentname = ImageName
      def CI_REGISTRY_IMAGE = registryHostname + "/" + projectname + "/"+ Region + "/" + componentname
      println("registryHostname is " + registryHostname)
      println("projectname is " + projectname)
      println("componentname is " + componentname)
      println("ReistryName is " + CI_REGISTRY_IMAGE)
      println("Tag is " + ArtifactTag)
        dockerBuild(ArtifactTag, CI_REGISTRY_IMAGE)
      }
    }
  }
}

