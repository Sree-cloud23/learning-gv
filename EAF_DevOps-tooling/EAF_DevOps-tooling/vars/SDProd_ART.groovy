// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = config.GitRepo
  def GtiBranch = config.GtiBranch
  def gitCommit
  def AppName = config.AppName
  def BUILD_NUMBER = env.BUILD_NUMBER
  def ImageName = config.ImageName
  def ReleaseDir = "deployment"
  def HostnameOverride = config.HostnameOverride
  def AgentTemplate = getAgentTemplate(config)
  def Region = config.Region
  def ConfigEnv = config.ConfigEnv
  def ArtifactTag = "${params.ArtifactTag}"
  


  print "GitRepo=${GitRepo} GtiBranch=${GtiBranch} ArtifactTag=${ArtifactTag}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "up-bitbucket")
       gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        echo "Checked out git commit ${gitCommit}"
      }
        
    stage('Get Binary')
      {
        container('utils') {
          withCredentials([usernamePassword(credentialsId: 'nexus-cred', passwordVariable: 'pw',  usernameVariable: 'user')]) {
          sh """
            echo "Downloading Binary Started"
            cd ${env.WORKSPACE}/
            mkdir -p ${AppName}/tmp/
            curl -u "${user}:${pw}" https://nexus.mgmt.ciap.xpaas.io/repository/${GLOBAL_BinaryArtifactName}/${ArtifactTag}/${AppName}.zip --output ${AppName}/tmp/${AppName}.zip
            unzip ${AppName}/tmp/${AppName}.zip -d ${AppName}/tmp/ && rm -fr ${AppName}/tmp/${AppName}.zip
            pwd && ls -lrht ${AppName}/ && ls
          """
          }
        }
      }
       def verCode = UUID.randomUUID().toString()
       def Dtag = verCode.substring(0,8)
       println(Dtag)
       def gitCommit2 = gitCommit+Dtag

    stage('Docker Build'){
      printTime("docker build image")
      def registryHostname =  "${env.BASE_HARBOR_REGISTRY}"
      def projectname = "up"
      def componentname = ImageName
      def CI_REGISTRY_IMAGE = registryHostname + "/" + projectname + "/" + componentname
      println("registryHostname is " + registryHostname)
      println("projectname is " + projectname)
      println("componentname is " + componentname)
      println("ReistryName is " + CI_REGISTRY_IMAGE)
      println("Tag is " + gitCommit2)
      container(name: 'kaniko', shell: '/busybox/sh') {
        sh """
          #!/busybox/sh
          /kaniko/executor --dockerfile ${AppName}/DockerfileART --build-arg Region=${Region} --build-arg ConfigEnv=${ConfigEnv} --build-arg AppName=${AppName} --context ./${AppName} \
            --snapshotMode redo \
            --destination ${CI_REGISTRY_IMAGE}:${gitCommit2}
          """
        }
      }
      /* Helm Release starts
      def exists = fileExists "${ReleaseDir}"
      if (!exists) {
        new File("${ReleaseDir}/").mkdir()
      }
      dir("${ReleaseDir}") {
        printTime("Sucessfully entired into ReleaseDir to get chart: ${ReleaseDir}")
        try {
          checkout([$class: 'GitSCM', branches: [[name: HelmBranch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'up-bitbucket', url: 'https://github.com/capgemini-gadm/UP-helm-chart.git']]])
        } catch(Exception err) {
          println("Unable to Retrive helm chart : ${err}")
          throw err
        }
      }
   // Helm Release ends
      stage('Helm Chart Verify') {
       verifyHelmChart(HelmEnv, ReleaseDir, HelmCharName, gitCommit2, NameSpace, "kube-jenkins-token-proving-ns-utility", "https://FB29D489E109EB6D4E96AEAD7068E334.gr7.eu-west-1.eks.amazonaws.com")
      }
   // Helm Deployemnt 
      stage('Helm Chart Deployment') {  
      deployHelmChart(HelmEnv, HelmName, ReleaseDir, HelmCharName, gitCommit2, NameSpace, "kube-jenkins-token-proving-ns-utility", "https://FB29D489E109EB6D4E96AEAD7068E334.gr7.eu-west-1.eks.amazonaws.com", BUILD_NUMBER, HostnameOverride)
      }*/
    }
  }
}
