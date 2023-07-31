// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def AgentTemplate = getAgentTemplate(config)
  print AgentTemplate
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    
    container(name: 'python') {
        sh """
          curl --location --request GET 'https://smartdispatcher.gadm.eks-test.eu.eafcore.com/healthcheck/'

        """
      }
    }
  }
}
