// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def AgentTemplate = getAgentTemplate(config)
  def TenanDBName = "${params.TenanDBName}"
  def Dburl = "${params.dbUrl}"
  def UserName = "${params.UserName}"
  def Query = "${params.Query}"
  def UserPassword = "${params.UserPassword}"

  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
    stage('DB script') {
      container(name: 'postgres') {
          sh '''
                export PGPASSWORD='''+UserPassword+'''
                #!/bin/sh
                psql -h '''+Dburl+''' -U '''+UserName+''' -p 5432 -d '''+TenanDBName+''' -c  '''+Query+'''
            '''
        }
      }
    }
  }
}
