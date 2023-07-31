// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants
def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = config.GitRepo
  def GitBranch = "${params.branch}"
  def gitCommit
  def ReleaseDir = "deployment"
  def AgentTemplate = getAgentTemplate(config)
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def Amdb_username = "${params.amdb_username}"
  def Amdb_password = "${params.amdb_password}"
  def Shareddb_username = "${params.shareddb_username}"
  def Shareddb_password = "${params.shareddb_password}"

  print "GitRepo=${GitRepo} GitBranch=${GitBranch}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GitBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
      }
    stage('VM script') {
      container(name: 'apim') {
          sh '''
            #!/bin/sh
            REGION='''+Region+'''
            Envir='''+Envir+'''
            DomainCall=`echo ${REGION}-${Envir}`
            wso2carbon_jks_pass=`tr -dc A-Za-z0-9 < /dev/urandom | head -c 10`
            client_truststore_jks_pass=`tr -dc A-Za-z0-9 < /dev/urandom | head -c 10`
            amdb_username='''+Amdb_username+'''
            amdb_password='''+Amdb_password+'''
            shareddb_username='''+Shareddb_username+'''
            shareddb_password='''+Shareddb_password+'''

            cp ${WSO2_SERVER_HOME}/repository/resources/security/client-truststore.jks /tmp/
            cp ${WSO2_SERVER_HOME}/repository/resources/security/wso2carbon.jks /tmp/

            rm -fr ${WSO2_SERVER_HOME}/repository/resources/security/client-truststore.jks
            rm -fr ${WSO2_SERVER_HOME}/repository/resources/security/wso2carbon.jks
            case ${DomainCall} in
                EU-QA)
                 input=`echo "test.eu.eafcore"`
                  ;;
                EU-PROD)
                  input=`echo "production.eu.eafcore"`
                  ;;
                NA-DEV)
                  input=`echo "sandpit.na.eafcore"`
                  ;;
                NA-QA)
                  input=`echo "test.na.eafcore"`
                  ;;
                NA-PROD)
                  input=`echo "production.na.eafcore"`
            esac
            ######Step-1#########
            echo "Generate wso2carbon.jks file"

            keytool -genkey -alias wso2carbon -keyalg RSA -keysize 2048 -validity 3650 -keystore wso2carbon.jks -dname "CN=*.gadm.eks-${input}.com,OU=CAPGEMINI,O=CAPGEMINI,L=INDIA,ST=SOUTH ,CN=IN" -ext "SAN=DNS:wso2am-pattern-4-am-cp-1-service,DNS:wso2am-pattern-4-am-cp-2-service,DNS:wso2am-pattern-4-am-cp-service,DNS:wso2am-pattern-4-am-external-gateway-service,DNS:wso2am-pattern-4-am-internal-gateway-service,DNS:wso2am-pattern-4-am-trafficmanager-1-service,DNS:wso2am-pattern-4-am-trafficmanager-2-service,DNS:wso2am-pattern-4-am-trafficmanager-service,DNS:wso2am-pattern-4-is-km-service,DNS:wso2am-pattern-4-mi-dashbard-service,DNS:wso2am-pattern-4-mi-service" -storepass  ${wso2carbon_jks_pass} -keypass ${wso2carbon_jks_pass}

            echo "Generate client-truststore.jks file"

            ######Step-2#########
            cp /tmp/client-truststore.jks `pwd`/
            keytool -storepasswd -new ${client_truststore_jks_pass} -keystore client-truststore.jks -storepass wso2carbon

            echo ${client_truststore_jks_pass} | keytool -delete -alias wso2carbon -keystore client-truststore.jks

            ######Step-3#########
            echo "Export public key from wso2carbon.jks file"

            keytool -export -alias wso2carbon -keystore wso2carbon.jks -file wso2carbonpub.pem -storepass ${wso2carbon_jks_pass}

            ######Step-4#########
            echo "Import public key into client-truststore.jks file"

            echo yes | keytool -import -alias wso2carbon -file wso2carbonpub.pem -keystore client-truststore.jks -storepass ${client_truststore_jks_pass}

            sed -i '28,33 {s/^/#/}' /home/wso2carbon/wso2am-4.1.0/repository/conf/deployment.toml
            #####Step-5#########
            echo "
[keystore.tls]
file_name =  "\\"wso2carbon.jks"\\"
type =  "\\"JKS"\\"
password =  "\\"${wso2carbon_jks_pass}"\\"
alias =  "\\"wso2carbon"\\"
key_password =  "\\"${wso2carbon_jks_pass}"\\"

[truststore]
file_name = "\\"client-truststore.jks"\\"
type = "\\"JKS"\\"
password = "\\"${client_truststore_jks_pass}"\\"

[keystore.primary]
file_name =  "\\"wso2carbon.jks"\\"
type = "\\"JKS"\\"
password = "\\"${wso2carbon_jks_pass}"\\"
alias =  "\\"wso2carbon"\\"
key_password = "\\"${wso2carbon_jks_pass}"\\"

[secrets]
amdb-username="\\"[${amdb_username}]"\\"
amdb-password="\\"[${amdb_password}]"\\"
shareddb-username="\\"[${shareddb_username}]"\\" 
shareddb-password="\\"[${shareddb_password}]"\\"
wso2-admin-user="\\"[${shareddb_password}]"\\" 
wso2-admin-password="\\"[${wso2-admin-password}]"\\" 
keystore-password="\\"[${wso2carbon_jks_pass}]"\\"
keystore-key-password="\\"[${wso2carbon_jks_pass}]"\\"
trustore-password="\\"[${client_truststore_jks_pass}]"\\"
primary-keystore-password="\\"[${wso2carbon_jks_pass}]"\\"
primary-keystore-key-password ="\\"[${wso2carbon_jks_pass}]"\\"
h2db_username="\\"[wso2carbon]"\\"
h2db_password="\\"[wso2carbon]"\\"
            " >>${WSO2_SERVER_HOME}/repository/conf/deployment.toml

            cat /home/wso2carbon/wso2am-4.1.0/repository/conf/deployment.toml

            cp -r client-truststore.jks ${WSO2_SERVER_HOME}/repository/resources/security/
            cp -r wso2carbon.jks ${WSO2_SERVER_HOME}/repository/resources/security/

            cd ${WSO2_SERVER_HOME}/bin
            /usr/bin/sh ./ciphertool.sh –Dconfigure -Dpassword=${wso2carbon_jks_pass}

            '''
        }
      }
    }
  }
}