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
   
      /*stage('ConfigUpdate') {
        dir('platform') {
              gitCheckout(
                branch: "${GtiBranchPlf}", url: "https://github.com/capgemini-gadm/${GitRepoPlf}", credentialsId: "git-devops")
        }
        sh "pwd && ls platform/"
        def exists = fileExists "platform/${AppName}/${Region}/${Proj}/${Envir}/"
        echo "===${exists}==="
        if (exists) {
          sh "cp -Rpv devops/helm-chart/config/${HelmEnv}/Dockerfile ."
          //sh "cp -Rpv platform/${AppName}/${Region}/${Proj}/${Envir}/configmap.yaml Config/"
          //sh "cat src/main/resources/configmap.yaml"
        }
      }*/
   
    stage('MavenBuild') {
      println("maven install")
      mavenExecution(
        buildParams : "-f EAFDirectConnectorIncidentProject/pom.xml install"
        )
      mavenExecution(
        buildParams : "-f EAFDirectConnectorSCTaskProject/pom.xml install"
        )
      }
    stage('Copy Car') {
          println "Copy Car files"
          fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: 'EAFDirectConnectorIncidentProject/*CompositeApplication/target/*.car', renameFiles: false, sourceCaptureExpression: '', targetLocation: 'Config/carbonapps/', targetNameExpression: '')])
          fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: 'EAFDirectConnectorSCTaskProject/*CompositeApplication/target/*.car', renameFiles: false, sourceCaptureExpression: '', targetLocation: 'Config/carbonapps/', targetNameExpression: '')])
          println "Copy Jar files"
          fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: 'EAFDirectConnectorIncidentProject/*/target/*.jar', renameFiles: false, sourceCaptureExpression: '', targetLocation: 'Config/lib/', targetNameExpression: '')])
          fileOperations([fileCopyOperation(excludes: '', flattenFiles: true, includes: 'EAFDirectConnectorSCTaskProject/*/target/*.jar', renameFiles: false, sourceCaptureExpression: '', targetLocation: 'Config/lib/', targetNameExpression: '')])    
      }

    stage('ConfigUpdate NA') {
      dir('platform') {
              gitCheckout(
                branch: "${GtiBranchPlf}", url: "https://github.com/capgemini-gadm/${GitRepoPlf}", credentialsId: "git-devops")
        }
        sh "pwd && ls platform/"
        def exists = fileExists "platform/${AppName}/${Proj}/NA/${Envir}/"
        echo "===${exists}==="
        if (exists) {
          sh "cp -Rpv platform/${AppName}/${Proj}/NA/${Envir}/* Config/conf/"
          sh "cp -Rpv  platform/${AppName}/DockerfileART Dockerfile && cat Dockerfile"
        
          //sh "cat src/main/resources/application.properties"
        }
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
      def CI_REGISTRY_IMAGE = registryHostname + "/" + projectname + "/"+ "na" + "/" + componentname
      println("registryHostname is " + registryHostname)
      println("projectname is " + projectname)
      println("componentname is " + componentname)
      println("ReistryName is " + CI_REGISTRY_IMAGE)
      println("Tag is " + ArtifactTag)
        dockerBuild(ArtifactTag, CI_REGISTRY_IMAGE)
      }
      /*stage('ConfigUpdate EU') {
      dir('platform') {
              gitCheckout(
                branch: "${GtiBranchPlf}", url: "https://github.com/capgemini-gadm/${GitRepoPlf}", credentialsId: "git-devops")
        }
        sh "pwd && ls platform/"
        def exists = fileExists "platform/${AppName}/${Proj}/EU/${Envir}/"
        echo "===${exists}==="
        if (exists) {
          sh "cp -Rpv platform/${AppName}/${Proj}/EU/${Envir}/* Config/conf/"
          sh "cp -Rpv  platform/${AppName}/DockerfileART Dockerfile && cat Dockerfile"
        
          //sh "cat src/main/resources/application.properties"
        }
      stage('Docker Build') {
      printTime("docker build image")
      def registryHostname =  "${env.BASE_REGISTRY}"
      def projectname = "capgemini-gadm"
      def componentname = ImageName
      def CI_REGISTRY_IMAGE = registryHostname + "/" + projectname + "/"+ "eu" + "/" + componentname
      println("registryHostname is " + registryHostname)
      println("projectname is " + projectname)
      println("componentname is " + componentname)
      println("ReistryName is " + CI_REGISTRY_IMAGE)
      println("Tag is " + ArtifactTag)
        dockerBuild(ArtifactTag, CI_REGISTRY_IMAGE)
      }*/
    }
  }
  }


