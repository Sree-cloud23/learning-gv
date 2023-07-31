// file: vars/greetings.groovy 
def call(body) {
  def config = [: ]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = "eaf_webui"
  //def GtiBranch = "develop"
  def GtiBranch = "${params.branch}"
  def RegistryName = "devciapregistry"
  def Registry_Cred = "azure-registry-dev"
  def ImageName = "webui"
  def gitCommit
  def BUILD_NUMBER = env.BUILD_NUMBER
  def EMID = "ciap-ui.in@capgemini.com,ciap-testing.in@capgemini.com"
  def KUBECNF = "keys/config_AUT-NL-AKS-DEV-CLU-01"
  def NameSpace = "gadm-commandtower"
  def HelmName = "webui-1"
  def HelmCharName = "webui"
  def HelmBranch = "develop"
  def ReleaseDir = "deployment"
  def AgentTemplate = getAgentTemplate(config)
  def Proj = "EAF"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def AppEnv = Proj + "/" + Region + "/" + Envir
  //HostnameOverride = "webui.gadm-dev.cs.usnc.eafcore.com"
  
  print "GitRepo=${GitRepo} GtiBranch=${GtiBranch} Envir=${Envir} Region=${Region} AppEnv=${AppEnv}"
  print AgentTemplate
  podTemplate(yaml: AgentTemplate.stripIndent()){
     node(POD_LABEL) {
      stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
      }
         
      // START: Docker tag random id
       def verCode = UUID.randomUUID().toString()
       def Dtag = verCode.substring(0,8)
       println(Dtag)
       def gitCommit2 = gitCommit+Dtag
      // END: Docker tag random id 
   
      
      stage('NodenBuild') {
      println("node install")
      nodeExecution(
        buildParams : "install && npm run build-stage"
        )
      }
      
    stage('Docker Build') {
        printTime("docker build image")
        //docker.withRegistry("https://${RegistryName}.azurecr.io", "${Registry_Cred}") {
         // def customImage = docker.build("${RegistryName}.azurecr.io/${ImageName}:${gitCommit2}")
          //customImage.push()
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
          /kaniko/executor --dockerfile Dockerfile --build-arg Region=${Region} --build-arg ConfigEnv=${Envir} --build-arg Proj=${Proj} --context \$(pwd) \
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
        echo 'Copying configmap.yaml values to the required release location'
        sh "pwd"
        sh "ls -lrht ${ReleaseDir}"
        //sh "cp devops/helm-chart/config/${AppEnv}/configmap.yaml ${ReleaseDir}/${HelmCharName}/devops/helm-chart/config/${AppEnv}/configmap.yaml"
        //sh " cp devops/helm-chart/${HelmCharName}/templates/configmap.yaml ${ReleaseDir}/${HelmCharName}/devops/helm-chart/${HelmCharName}/templates/configmap.yaml"
        echo 'File is copied successfully'
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
