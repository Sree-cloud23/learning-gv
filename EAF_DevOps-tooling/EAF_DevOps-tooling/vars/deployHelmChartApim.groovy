#!/usr/bin/groovy

def call(String AppEnv, String HelmName, String ReleaseDir, String HelmCharName, String gitCommit2, String NameSpace, String ClusterName,  String BUILD_NUMBER, String Region, String Envir) {
container('utils') {
        printTime("Helm Deployment to ${AppEnv}")
        sh "${ClusterName}"
        dir("${ReleaseDir}/${HelmCharName}/advanced") {
        printTime("Sucessfully entired into ReleaseDir for release : ${ReleaseDir}/${HelmCharName}")
       
          retry(2) {
            //withKubeConfig(credentialsId: clusterCredentials, serverUrl: clusterAddress) {
            sh "helm list --namespace ${NameSpace}"
            //}
          }
          retry(2) {
            printTime("Helm Deployment to Development")
            sh "helm dependency build am-pattern-4/"
            sh "helm upgrade --install ${HelmName} am-pattern-4/  --namespace ${NameSpace} --values devops/helm-chart/config/${AppEnv}/values.yaml --values devops/helm-chart/config/${AppEnv}/db_secret.yaml --set-string wso2.deployment.am.imageTag=${gitCommit2} --set-string wso2.deployment.am.Envir=${Envir} --set-string wso2.deployment.am.Region=${Region} --description jenkins-build-${BUILD_NUMBER}"
            //}
          }
          
        }
}
}
