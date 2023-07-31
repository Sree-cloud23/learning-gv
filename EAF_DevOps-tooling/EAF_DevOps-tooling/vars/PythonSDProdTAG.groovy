// file: vars/greetings.groovy y
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants

def call(body) {
  def config = [: ]

  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def GitRepo = config.GitRepo
  def GitRepoPlf = config.GitRepoPlf
  def GtiBranchPlf = config.GtiBranchPlf
  def GtiBranch = "${params.branch}"
  def Registry_Cred = config.Registry_Cred
  def ImageName = config.ImageName
  def gitCommit
  def AppName = config.AppName
  def Proj = config.Proj
  def Envir = config.Envir
  def HostnameOverride = config.HostnameOverride
  def ArtifactTag = "${params.ArtifactTag}"
  def AgentTemplate = getAgentTemplate(config)


    print "GitRepo=${GitRepo} GtiBranch=${GtiBranch} GitRepoPlf=${GitRepoPlf} GtiBranchPlf=${GtiBranchPlf} Envir=${Envir}"
  
  print AgentTemplate
  
  podTemplate(yaml: AgentTemplate.stripIndent()){
  node(POD_LABEL) {
   stage('GetCode') {
        printTime("Checkout code")
        gitCheckout(
        branch: "${GtiBranch}", url: "https://github.com/capgemini-gadm/${GitRepo}", credentialsId: "git-devops")
        gitCommit = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        sh "pwd && ls"
      }
   
      def zone = ['NA', 'EU']
      stage('ConfigUpdate ForLoop') {
          for (int i = 0; i < zone.size(); ++i) 
          {
           def Region = "${zone[i]}"
           dir('platform') {
              gitCheckout(
                           branch: "${GtiBranchPlf}", url: "https://github.com/capgemini-gadm/${GitRepoPlf}", credentialsId: "git-devops")
                          }
                            def exists = fileExists "platform/${AppName}/${Proj}/${Region}/${Envir}/"
                            echo "===${exists}==="
                            if (exists) {
                                sh "cp -Rpv platform/${AppName}/${Proj}/${Region}/${Envir}/config/* conf_model_dir/"
                                sh "cp -Rpv  platform/${AppName}/DockerfileART Dockerfile"
                                //sh "cat src/main/resources/application.properties"
                              }
                            def verCode = UUID.randomUUID().toString()
                            def Dtag = verCode.substring(0,8)
                            println(Dtag)
                            def gitCommit2 = ArtifactTag
                            // END: Docker tag random id 
                         
                            printTime("docker build image")
                            def registryHostname =  "${env.BASE_REGISTRY}"
                            def projectname = "capgemini-gadm"
                            def componentname = ImageName
                            def CI_REGISTRY_IMAGE = registryHostname + "/" + projectname + "/" + Region.toLowerCase() + "/" + Envir.toLowerCase() + "/" + componentname
                            println("registryHostname is " + registryHostname)
                            println("projectname is " + projectname)
                            println("componentname is " + componentname)
                            println("ReistryName is " + CI_REGISTRY_IMAGE)
                            println("Tag is " + ArtifactTag)
                            container(name: 'kaniko', shell: '/busybox/sh') {
                                  sh """
                                    #!/busybox/sh
                                    /kaniko/executor --dockerfile Dockerfile --build-arg Region=${Region} --build-arg ConfigEnv=${Envir} --build-arg Proj=${Proj} --context \$(pwd) \
                                      --snapshot-mode redo \
                                      --destination ${CI_REGISTRY_IMAGE}:${gitCommit2}
                                    """
                        }
                }
          }
    }
  }
}

