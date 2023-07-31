// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def gitCommit
  def AppName = config.AppName
  def BUILD_NUMBER = env.BUILD_NUMBER
  def ImageName = config.ImageName
  def ReleaseDir = "deployment"
  def HostnameOverride = config.HostnameOverride
  def AgentTemplate = getAgentTemplate(config)
  //def Region = config.Region
  def ConfigEnv = config.ConfigEnv
  def ArtifactTag = "${params.ArtifactTag}"
  def HelmBranch = config.HelmBranch
  def NameSpace = config.NameSpace
  def HelmName = config.HelmName
  def HelmCharName = config.HelmCharName
  def HelmEnv = config.HelmEnv
  def ClusterCred = config.ClusterCred
  def Proj = "EAF"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def AppEnv = Proj + "/" + Region + "/" + Envir

  print "Region=${Region} ArtifactTag=${ArtifactTag}"
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
      dir("${ReleaseDir}") {
        printTime("Sucessfully entired into ReleaseDir to get chart: ${ReleaseDir}")
        try {
          checkout([$class: 'GitSCM', branches: [[name: HelmBranch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git-devops', url: 'https://github.com/capgemini-gadm/EAF_helm_chart.git']]])
        } catch(Exception err) {
          println("Unable to Retrive helm chart : ${err}")
          throw err
        }
      }
      stage('Helm Chart Verify') {
       verifyHelmChart(AppEnv, ReleaseDir, HelmCharName, ArtifactTag, NameSpace)
      }
   
   // Helm Deployemnt 
      stage('Helm Chart Deployment') {  
      deployHelmChart(AppEnv, HelmName, ReleaseDir, HelmCharName, ArtifactTag, NameSpace, Cluster_Name(config), BUILD_NUMBER, Region.toLowerCase(), Envir.toLowerCase())
      }
    }
  }
}
