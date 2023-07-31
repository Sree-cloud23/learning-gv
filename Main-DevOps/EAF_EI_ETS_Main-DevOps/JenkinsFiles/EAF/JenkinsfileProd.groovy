#!groovy
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants
@Library('git-devops-libraries@tooling')_
EiProdDeploy{
   GitRepoPlf = "EAF_Platform_Config"
   GtiBranchPlf = "release"
   ImageName = "eiets"
   NameSpace = "gadm-eicustomerconnector"
   HelmBranch = "develop"
   HelmName = "ei-etsmain-1"
   HelmCharName = "microeiclient"
   ComponentName = "ETSMain"
   AppName = "EI_ETS_Main"
      
  repoType = Constants.REPO_TYPE_MAVEN
  kanikoContainerEnabled = false
  mavenContainerEnabled = false
  nodeContainerEnabled = false
}
