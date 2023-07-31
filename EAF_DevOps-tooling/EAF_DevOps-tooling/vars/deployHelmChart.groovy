#!/usr/bin/groovy

def call(String AppEnv, String HelmName, String ReleaseDir, String HelmCharName, String gitCommit2, String NameSpace, String ClusterName,  String BUILD_NUMBER, String Region, String Envir) {
container('utils') {
        printTime("Helm Deployment to ${AppEnv}")
        sh "${ClusterName}"
        dir("${ReleaseDir}/${HelmCharName}") {
        printTime("Sucessfully entired into ReleaseDir for release : ${ReleaseDir}/${HelmCharName}")
       
          retry(2) {
            //withKubeConfig(credentialsId: clusterCredentials, serverUrl: clusterAddress) {
            sh "helm list --namespace ${NameSpace}"
            //}
          }
          retry(2) {
            printTime("Helm Deployment to Development")
            sh "helm upgrade --install ${HelmName} devops/helm-chart/${HelmCharName}  --namespace ${NameSpace} --values devops/helm-chart/config/${AppEnv}/values.yaml --set-string image.tag=${gitCommit2} --set-string image.Envir=${Envir} --set-string image.Region=${Region} --description jenkins-build-${BUILD_NUMBER}"
            //}
           }
          }
         }
        }
