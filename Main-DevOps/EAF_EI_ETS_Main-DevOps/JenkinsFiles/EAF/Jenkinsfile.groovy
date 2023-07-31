#!groovy
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants
@Library('git-devops-libraries@tooling')_
EI_Main_ClientTemplate{
   CiapDeployer = "ver-2.1"
   GitRepo = "EAF_EI_ETS_Main"
   ImageName = "eiets"
   NameSpace = "gadm-eicustomerconnector"
   HelmBranch = "develop"
   HelmName = "ei-etsmain-1"
   HelmCharName = "microeiclient"
   ComponentName = "ETSMain"
   IngressName = "etsmain"
   
  repoType = Constants.REPO_TYPE_MAVEN
  kanikoContainerEnabled = true
  mavenContainerEnabled = true
  nodeContainerEnabled = false
}
