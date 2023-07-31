#!/usr/bin/groovy

def call(String AppEnv, String ReleaseDir, String HelmCharName, String gitCommit2, String NameSpace) {
container('utils') {
   // Helm Release ends
   printTime("Helm Chart Verify")
    dir("${ReleaseDir}/${HelmCharName}/advanced") {
      printTime("Sucessfully entired into ReleaseDir for Chart verification: ${ReleaseDir}/${HelmCharName}/advanced")
      try {
        //withKubeConfig(credentialsId: clusterCredentials, serverUrl: clusterAddress) {
          def helm = sh(script: "helm lint am-pattern-4/ --values devops/helm-chart/config/${AppEnv}/values.yaml --values devops/helm-chart/config/${AppEnv}/db_secret.yaml", returnStdout: true).trim() 
        //}
      } catch(Exception err) {
        println("Unable to validate helm chart : ${err}")
        throw err
      }
    }
  }
}
