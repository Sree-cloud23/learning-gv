  // file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def CiapDeployer = config.CiapDeployer
  def GitRepo = config.GitRepo
  def GtiBranch = "${params.branch}"
  def RegistryName = config.RegistryName
  def Registry_Cred = config.Registry_Cred
  def ImageName = config.ImageName
  def gitCommit
  def BUILD_NUMBER = env.BUILD_NUMBER
  def EMID = config.EMID
  def KUBECNF = config.KUBECNF
  def NameSpace = config.NameSpace
  def HelmBranch = config.HelmBranch
  def HelmName = config.HelmName
  def HelmCharName = config.HelmCharName
  def HelmEnv = config.HelmEnv
  def ReleaseDir = "deployment"
  def HostnameOverride = config.HostnameOverride
  def AgentTemplate = getAgentTemplate(config)
  def Proj = "EAF"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def AppEnv = Proj + "/" + Region + "/" + Envir

  print "GitRepo=${GitRepo} GtiBranch=${GtiBranch} Envir=${Envir} Region=${Region} AppEnv=${AppEnv}"
  print AgentTemplate.stripIndent()
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
   stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
      }
      
    // START: Copy environment config files
      /*stage('ConfigUpdate') {
        println "My Cluster NAame is " + Cluster_Name(config)
        def exists = fileExists "src/main/resources/${AppEnv}"
        echo "===${exists}==="
        if (exists) {
          sh "pwd"
          sh "cp -Rpv src/main/resources/${AppEnv}/* src/main/resources/"
          
        }
      }
   
    stage('MavenBuild') {
      println("maven install")
      mavenExecution(
        buildParams : "clean install"
        )
      }*/
   
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
        container(name: 'kaniko', shell: '/busybox/sh') {
          sh """
            #!/busybox/sh
            /kaniko/executor --dockerfile Dockerfile --build-arg Region=${Region} --build-arg Envir=${Envir} --build-arg Proj=${Proj} --context \$(pwd) \
              --snapshot-mode redo \
              --destination ${CI_REGISTRY_IMAGE}:${gitCommit2}
            """
          }
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
        echo 'Copying Config related yaml values to the required release location'
        sh "pwd"
        sh "ls -lrht ${ReleaseDir}"
        sh "cp devops/helm-chart/config/${AppEnv}/db_secret.yaml ${ReleaseDir}/${HelmCharName}/advanced/devops/helm-chart/config/${AppEnv}/db_secret.yaml"
        echo 'File is copied successfully'
      }
      catch(Exception e) {
        error 'Copying configmap.yaml values file was unsuccessful'
      }
       // Helm Release ends
        stage('Helm Chart Verify') {
         verifyHelmChartApim(AppEnv, ReleaseDir, HelmCharName, gitCommit2, NameSpace)
        }
   
      // Helm Deployemnt 
        stage('Helm Chart Deployment') {  
        deployHelmChartApim(AppEnv, HelmName, ReleaseDir, HelmCharName, gitCommit2, NameSpace, Cluster_Name(config), BUILD_NUMBER, Region.toLowerCase(), Envir.toLowerCase())
      }
    }
  }
}


