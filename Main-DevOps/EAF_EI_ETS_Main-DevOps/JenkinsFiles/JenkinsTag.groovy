#!groovy
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants
@Library('git-devops-libraries@tooling')_
EiClientProdTAG{
   GitRepo = "EAF_EI_ETS_Main"
   GitRepoPlf = "EAF_Platform_Config"
   GtiBranchPlf = "release"
   ImageName = "eiets" 
   HelmBranch = "develop"
   AppName = "EI_ETS_Main"
   Proj = "EAF"
   Envir = "PROD"
   
  repoType = Constants.REPO_TYPE_MAVEN
  kanikoContainerEnabled = true
  mavenContainerEnabled = true
  nodeContainerEnabled = false
  azureContainerEnabled = false
}
