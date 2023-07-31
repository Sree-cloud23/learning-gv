#!/usr/bin/groovy

def loginIntoEnv(env){
         withCredentials([usernamePassword(credentialsId: 'apimipass', passwordVariable: 'password', usernameVariable: 'userName')]) {
         apim_tenant_login = "apictl login -u "+"${userName}"+" -p "+"${password}"+" "+"${env}"+" -k"
         sh apim_tenant_login
         println "Successfully logged into Env!!!"
         }
}
def logoutFromEnv(env){
    apim_tenant_logout = "apictl logout "+"${env}"+" -k"
    sh apim_tenant_logout
    println "Successfully logged out from Env!!!"   
}
def call(Map apictlconfig1) {
container('utils') {
   withCredentials([usernamePassword(credentialsId: 'nexus-robot', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
    println("====${apictlconfig1.apictlconfig}====")
    sh "apictl ${apictlconfig1.apictlconfig}"
    checkout([$class: 'GitSCM', branches: [[name: Branch]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout', deleteUntrackedNestedRepositories: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: './UP-APIM-Main'], [$class: 'LocalBranch', localBranch: '**']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'balasaheb_bit_ucket', url: 'https://github.com/capgemini-gadm/up_apim_main.git']]])
	 println "checkout completed"
    echo 'copying config files to Base'
    //fileOperations([folderCopyOperation(destinationFolderPath: 'UP-APIM-Main', sourceFolderPath: "${Tenant_specific_config_location}")])
    //println "config setup done..."
    sh "ls -lrt" 
    def apim_config = readYaml file: '${Tenant_specific_config_location}/deployment.yaml'
    def apisToDeploy = apim_config.deploy_apis
        //sh 'bash ./config.sh'
        if(apisToDeploy!=null){
        loginIntoEnv("${apim_environment}")
        apisToDeploy.each{Module,apisList->
            println "Deploying APIs of Module :"+"$Module"
            def apis=apisList;
            for(String api:apis){
            import_api = "apictl import-api -f "+"${Tenant_specific_config_location}/api/"+"${api}"+" -e "+"${Apim_environment}"+" -k --preserve-provider=false --update"
            sh import_api
            println "$api"+ " imported"
            }
                    }
        logoutFromEnv("${apim_environment}")
        println "APIs deployed"
        }
        else{
         println "No APIs are configured to deploy"
        }
         def apim_config1 = readYaml file: './UP-APIM-Main/deployment.yaml'
        def appsToDeploy = apim_config1.deploy_apps
        if(appsToDeploy!=null){
        loginIntoEnv("${apim_environment}")
        for(String app:appsToDeploy){
            println "Deploying APP : "+ "$app"
            import_app = "apictl import-app -f "+"./UP-APIM-Main/apps/"+"${app}.zip"+" -s=true"+" -e "+"${apim_environment}"+" -k --update"
             sh import_app
             println "$app"+ " imported"
             }
        logoutFromEnv("${apim_environment}")
        println "Apps deployed"
        }
        else{
        println "No Apps are configured to deploy"
        }
    }
  }
 }

