// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def AgentTemplate = getAgentTemplateSD(config)
  def TenantName = "${params.TenantName}"
  def TrainType = "${params.TrainType}"
  def TenantVersion = "${params.TenantVersion}"
  def Envir = "${params.Envir}"
  def Region = "${params.Region}"
  def Aws_region = "${params.aws_region}"
  def Model_Name = "${params.Model_Name}"
  def Version_nu = "${params.Version}"
  def Languages = "${params.languages}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    container(name: 'python') {
        sh """
        curl --location --request POST '${S3}' --header 'Content-Type: application/json'  --header 'Connection: close' --data '{"model_name": "${Model_Name}" , "model_lang": "${Languages}" , "version": "${Version_nu}"}'
        """
      }
    }
  }
}
