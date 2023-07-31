def call(body) {
  def config = [: ]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = "eaf_SmartDispatcher"
  def GtiBranch = "${params.branch}"
  def ImageName = "smartdispatcher"
  def gitCommit
  def BUILD_NUMBER = env.BUILD_NUMBER
  def EMID = "ciap-ui.in@capgemini.com,ciap-testing.in@capgemini.com"
  def NameSpace = "gadm-smartdispatcher"
  def HelmName = "smartdispatcher-1"
  def HelmCharName = "smartdispatcher"
  def HelmBranch = "develop"
  def ReleaseDir = "deployment"
  def HostnameOverride = "smartdispatcher"
  def AgentTemplate = getAgentTemplate(config)
  def Proj = "EAF"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def AppEnv = Proj + "/" + Region + "/" + Envir
  def IngressName = "smartdispatcher"
  def IngressName = config.IngressName
  def HostnameOverride = IngressName + Ingress_Name(config)
  
  print "GitRepo=${GitRepo} GtiBranch=${GtiBranch} Envir=${Envir} Region=${Region} AppEnv=${AppEnv}"

  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
 
    node(POD_LABEL) {
    //properties([[$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '10', numToKeepStr: '10']]]);
    /*properties([
    disableConcurrentBuilds(), parameters([
    string(defaultValue: 'develop', name: 'branch')])])*/
     
      stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        echo "Checked out git commit ${gitCommit}"
      }
         
      // START: Copy environment config files
     stage('ConfigUpdate') {
        def exists = fileExists "devops/helm-chart/config/${AppEnv}"
        echo "===${exists}==="
        if (exists) {
          sh "pwd"
          sh "cp -Rpv devops/helm-chart/config/${AppEnv}/* ."
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
      def CI_REGISTRY_IMAGE = registryHostname + "/" + projectname + "/" + Region.toLowerCase() + "/" + Envir.toLowerCase() + "/" + componentname
      println("registryHostname is " + registryHostname)
      println("projectname is " + projectname)
      println("componentname is " + componentname)
      println("ReistryName is " + CI_REGISTRY_IMAGE)
      println("Tag is " + gitCommit2)
        dockerBuild(gitCommit2, CI_REGISTRY_IMAGE)
        }
     // Helm Release starts
      def exists = fileExists "${ReleaseDir}"
      if (!exists) {
        new File("${ReleaseDir}/").mkdir()
      }
      dir("${ReleaseDir}") {
        printTime("Sucessfully entired into ReleaseDir to get chart: ${ReleaseDir}")
        try {
          checkout([$class: 'GitSCM', branches: [[name: HelmBranch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git-devops', url: 'https://github.com/capgemini-gadm/EAF_helm_chart.git']]])
        } catch(Exception err) {
          println("Unable to Retrive helm chart : ${err}")
          throw err
        }
      }
      try {
        echo 'Copying configmap.yaml values to the required release location'
        sh "pwd"
        sh "ls -lrht ${ReleaseDir}"
      }
      catch(Exception e) {
        error 'Copying configmap.yaml values file was unsuccessful'
      }
   
   // Helm Release ends
      stage('Helm Chart Verify') {
       verifyHelmChart(AppEnv, ReleaseDir, HelmCharName, gitCommit2, NameSpace)
      }
   
   // Helm Deployemnt 
    stage('Helm Chart Deployment') {  
    deployHelmChart(AppEnv, HelmName, ReleaseDir, HelmCharName, gitCommit2, NameSpace, Cluster_Name(config), BUILD_NUMBER, Region.toLowerCase(), Envir.toLowerCase())
    }
 }
}
    }
