#!groovy

def call(Map stageParams) {
    checkout([
            $class                           : 'GitSCM',
            branches                         : [[name: stageParams.branch]],
            doGenerateSubmoduleConfigurations: false,
            extensions                       : [],
            submoduleCfg                     : [],
            userRemoteConfigs                : [[credentialsId: stageParams.credentialsId , url: stageParams.url]]
    ])
}
