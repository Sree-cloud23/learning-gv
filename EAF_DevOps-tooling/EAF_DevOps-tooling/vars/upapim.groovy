// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def loginIntoEnv(env,cred) {
  withCredentials([usernamePassword(credentialsId: "${cred}", passwordVariable: 'password', usernameVariable: 'userName')]) {
    apim_tenant_login = 'apictl login -u ' + "${userName}" + ' -p ' + "${password}" + ' ' + "${env}" + ' -k'
       sh apim_tenant_login
    println 'Successfully logged into Env!!!'
  }
}
def logoutFromEnv(env) {
  apim_tenant_logout = 'apictl logout ' + "${env}" + ' -k'
  sh apim_tenant_logout
  println 'Successfully logged out from Env!!!'
}
def call(body) {
  def config = [: ]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  def AgentTemplate = getAgentTemplate(config)
  //def Apim_tenant_login = "${env.apim_tenant_login}"
  def Branch = "${params.branch}"
  //def Apim_environment = "${env.apim_environment}"
  def Proj = "${params.TenantName}"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def APIM_Server = "${params.APIM_Server}"
  def APIM_Token =  "${params.APIM_Token}"
  def AppEnv = Proj + '/' + Region + '/' + Envir
  def Env = Proj.toLowerCase() + '-' + Region.toLowerCase() + '-' + Envir.toLowerCase()
  def Cred = config.cred
  

  //print "Branch=${Branch}  Apim_tenant_login=${Apim_tenant_login} Env=${Env}"
  print "Branch=${Branch}  Env=${Env}"

  print AgentTemplate
  podTemplate(yaml: AgentTemplate.stripIndent()) {
    node(POD_LABEL) {
      stage('Test Hello') {
        echo 'hello'
      }
      stage('Code checkout') {
        checkout([$class: 'GitSCM', branches: [[name: Branch]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout', deleteUntrackedNestedRepositories: true], [$class: 'RelativeTargetDirectory', relativeTargetDir: './EAF_APIM_Main_4.1.0'], [$class: 'LocalBranch', localBranch: '**']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'git-devops', url: 'https://github.com/capgemini-gadm/EAF_APIM_Main_4.1.0.git']]])
        println 'checkout completed'
      }

      stage('config apictl') {
        println('configuring apictl')
        container('utils') {
          sh 'apictl version'
          //withCredentials([usernamePassword(credentialsId: 'nexus-robot', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD')]) {
          sh "apictl add-env -e ${Env} --apim ${APIM_Server}"
        //}
        stage('Config setup') {
          echo 'copying config files to Base'
          //fileOperations([folderCopyOperation(destinationFolderPath: './EAF_APIM_Main_4.1.0', sourceFolderPath: "devops/helm-chart/config/${AppEnv}")])
          //sh "cp -Rpv devops/helm-chart/config/${AppEnv}/ ./EAF_APIM_Main_4.1.0/"
          fileOperations([folderCopyOperation(destinationFolderPath: 'EAF_APIM_Main_4.1.0', sourceFolderPath: "EAF_APIM_Main_4.1.0/devops/helm-chart/config/${AppEnv}/")])
          println 'config setup done...'
        }
        //sh "pwd;sleep 500"
        stage('Deploy APIs') {
          def apim_config = readYaml file: './EAF_APIM_Main_4.1.0/deployment.yaml'
          def apisToDeploy = apim_config.deploy_apis
          //sh 'bash ./config.sh'
          if (apisToDeploy != null) {
            loginIntoEnv("${Env}","${Cred}")
            apisToDeploy.each { Module, apisList->
              println 'Deploying APIs of Module :' + "$Module"
              def apis = apisList
              for (String api:apis) {
                //import_api = "apictl import-api -f "+"./EAF_APIM_Main_4.1.0/api/"+"${api}"+" -e "+"${Env}"+" -k --preserve-provider=false --update"
                import_api = "apictl import api -f "+"./EAF_APIM_Main_4.1.0/api/"+"${api}"+" -e "+"${Env}"+" --params "+"./EAF_APIM_Main_4.1.0/api/" + "${api}"+"/api_params.yaml"+" --preserve-provider=false --rotate-revision --update -k"
                println '${import_api}'
                sh import_api
                println "$api" + ' imported'
              }
            }
            logoutFromEnv("${Env}")
            println 'APIs deployed'
          }
        else {
            println 'No APIs are configured to deploy'
        }
        }
        stage('Deploy Apps') {
          def apim_config = readYaml file: './EAF_APIM_Main_4.1.0/deployment.yaml'
          def appsToDeploy = apim_config.deploy_apps
          if (appsToDeploy != null) {
            loginIntoEnv("${Env}","${Cred}")
            for (String app:appsToDeploy) {
              println 'Deploying APP : ' + "$app"
              //import_app = "apictl import-app -f "+"./EAF_APIM_Main_4.1.0/apps/"+"${app}.zip"+" -s=true"+" -e "+"${Env}"+" -k --update"
              import_app = 'apictl import app -f ' + './EAF_APIM_Main_4.1.0/apps/' + "${app}.zip" + ' -e ' + "${Env}" + ' -s=false --preserve-owner=true -k --update'
              //import_app = 'apictl import app -f ' + './EAF_APIM_Main_4.1.0/apps/' + "${app}.zip" + ' -e ' + "${Env}" + ' -s=false -k --update'
              sh import_app
              println "$app" + ' imported'
            }
            logoutFromEnv("${Env}")
            println 'Apps deployed'
          }
        else {
            println 'No Apps are configured to deploy'
        }
        }
        }
      }
    }
  }
}