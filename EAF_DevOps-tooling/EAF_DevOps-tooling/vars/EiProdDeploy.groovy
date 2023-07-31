// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def gitCommit
  def GitRepo = config.GitRepo
  def GitRepoPlf = config.GitRepoPlf
  def GtiBranchPlf = config.GtiBranchPlf
  def BUILD_NUMBER = env.BUILD_NUMBER
  def ImageName = config.ImageName
  def ReleaseDir = "deployment"
  def HostnameOverride = config.HostnameOverride
  def AgentTemplate = getAgentTemplate(config)
  def Region = "${params.Region}"
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
  def ComponentName = config.ComponentName
  def AppEnv = Proj + "/" + Region + "/" + Envir + "_" + ComponentName
  def ConfEnv = Proj + "/" + Region + "/" + Envir
  def AppName = config.AppName
  
  print "Region=${Region} ArtifactTag=${ArtifactTag}"
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    stage('Docker Build') {
        gitCheckout(
           branch: "${GtiBranchPlf}", url: "https://github.com/capgemini-gadm/${GitRepoPlf}", credentialsId: "git-devops")
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
              sh "ls && ls -lrht ${ReleaseDir}"
              sh "cp ${AppName}/${ConfEnv}/configmap.yaml ${ReleaseDir}/${HelmCharName}/devops/helm-chart/config/${AppEnv}/configmap.yaml"
              sh "cp ${AppName}/devops/helm-chart/${HelmCharName}/templates/configmap.yaml ${ReleaseDir}/${HelmCharName}/devops/helm-chart/${HelmCharName}/templates/configmap.yaml"
              echo 'File is copied successfully'
            }
        catch(Exception e) {
          error 'Copying configmap.yaml values file was unsuccessful'
          }
      stage('Helm Chart Verify') {
       verifyHelmChartEI(AppEnv, ReleaseDir, HelmCharName, ArtifactTag, NameSpace)
      }
   
   // Helm Deployemnt 
      stage('Helm Chart Deployment') {  
      deployHelmChartEI(AppEnv, HelmName, ReleaseDir, HelmCharName, ArtifactTag, NameSpace,Cluster_Name(config), BUILD_NUMBER, Region.toLowerCase(), Envir.toLowerCase())
        }
      }
    }
  }
