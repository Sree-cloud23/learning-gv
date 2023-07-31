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
  def Directory = "${params.Directory}"
  def S3bucket = "${params.s3_bucket}"

  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    container(name: 'python') {
        sh """
        curl --location --request POST '${S3}' --header 'tenant_name: ${TenantName}' --header 'Content-Type: application/json'  --header 'Connection: close' --data-raw '{ "version": "${TenantVersion}" , "trainType": "${TrainType}" , "Directory": "${Directory}" , "s3_bucket": "${S3bucket}" , "aws_region": "${Aws_region}"}'
        
        """
      }
    }
  }
}
