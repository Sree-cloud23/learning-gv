#!/usr/bin/env groovy 
import com.capgemini.nkaas.jenkins.pipeline.library.common.Constants
import com.capgemini.nkaas.jenkins.pipeline.library.common.StringExtensions

/**
 * Get the agent template to use for the build.
 */
def call(Map config = [:]) {
  println 'Generating pod template'
  println 'nodeContainerEnabled is ' + config.nodeContainerEnabled
  println 'mavenContainerEnabled is ' + config.mavenContainerEnabled
  println 'pythonContainerEnabled is ' + config.pythonContainerEnabled
  println 'postgresContainerEnabled is ' + config.postgresContainerEnabled
  println 'kanikoContainerEnabled is ' + config.kanikoContainerEnabled
  
  StringBuilder podTemplate = StringBuilder.newInstance()

  podTemplate << '''\
    apiVersion: v1
    kind: Pod
    spec:
  '''.stripIndent()

  podTemplate << StringExtensions.indent(getServiceAccountAndSecurityContext(config), 2)

  podTemplate << StringExtensions.indent("""\
      imagePullSecrets:
      - name: docker-registry
      containers:
      - name: utils
        image:  ${env.BUILD_UTILITIES_IMAGE}
        imagePullPolicy: Always
        resources:
          requests:
            memory: 20Mi
            cpu: 10m
          limits:
            memory: 250Mi
            cpu: 200m
        command:
        - cat
        tty: true
        env:
        - name: BUILD_SUB_PATH
          value: ${env.JOB_NAME}
  """.stripIndent(), 2)
 
 podTemplate << StringExtensions.indent(getJnlpContainer(config), 2)
 podTemplate << StringExtensions.indent(getKanikoContainer(config), 2)
 podTemplate << StringExtensions.indent(getMavenContainer(config), 2)
 //podTemplate << StringExtensions.indent(getNodeContainer(config), 2)
 podTemplate << StringExtensions.indent(getPythonContainer(config), 2)
 //podTemplate << StringExtensions.indent(getPostgresContainer(config), 2)
 //podTemplate << StringExtensions.indent(getGradleContainer(config), 2)
  podTemplate << StringExtensions.indent(getVolumes(config), 2)
  //podTemplate << StringExtensions.indent(getVolumeMounts(config), 2)

  return podTemplate.toString()
}

String getJnlpContainer(Map config) {
  return """\
      - name: jnlp
        image: jenkins/inbound-agent:4.11.2-4
        args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
        imagePullPolicy: Always
        resources:
          requests:
            memory: 100Mi
            cpu: 10m
          limits:
            memory: 512Mi
            cpu: 500m
    """.stripIndent()
}

String getServiceAccountAndSecurityContext(Map config = [:]) {
    return '''serviceAccountName: ''' + getServiceAccount(config) + '''
      securityContext:
        runAsUser: 0
    '''.stripIndent()
}

String getKanikoContainer(Map config) {
  if (config.kanikoContainerEnabled) {
    return """\
      - name: kaniko
        image: gcr.io/kaniko-project/executor:debug
        imagePullPolicy: Always
        command:
        - /busybox/cat
        tty: true
        volumeMounts:
        - name: docker-config
          mountPath: /kaniko/.docker
        resources:
          requests:
            memory: 250Mi
            cpu: 500m
          limits:
            memory: 3000Mi
            cpu: 1500m
    """.stripIndent()
  }
}

/*String getKanikoContainer(Map config) {
    return """\
      - name: kaniko
        image: gcr.io/kaniko-project/executor:debug
        imagePullPolicy: Always
        command:
        - /busybox/cat
        tty: true
        volumeMounts:
        - name: docker-config
          mountPath: /kaniko/.docker
     """.stripIndent()
}*/

String getMavenContainer(Map config) {
   if (config.mavenContainerEnabled) {
    return """\
      - name: maven
        image: maven:3.6.3-jdk-8
        command:
        - cat
        tty: true
        env:
        - name: BUILD_SUB_PATH
          value: ${env.JOB_NAME}
        - name: MAVEN_OPTS
          value: -Xmx4096M
        volumeMounts:
          - name: build-cache
            mountPath: /root/.m2
            subPathExpr: \$(BUILD_SUB_PATH)/.m2
        resources:
          requests:
            memory: 5000Mi
            cpu: 250m
          limits:
            memory: 8000Mi
            cpu: 1000m
    """.stripIndent()
   }
 }

String getPythonContainer(Map config) {
   if (config.pythonContainerEnabled) {
    return """\
      - name: python
        image: ghcr.io/capgemini-gadm/tools/smartdispatcher_base_3.9:2023-06-01
        command:
        - cat
        tty: true
        volumeMounts:
          - name: sd-upload
            mountPath: /cache
        resources:
          requests:
            memory: 50Mi
            cpu: 25m
          limits:
            memory: 500Mi
            cpu: 100m
    """.stripIndent()
   }
 }

/*String getNodeContainer(Map config) {
  if (config.nodeContainerEnabled) {
     return """\
      - name: node
        image: node:lts-alpine
        command:
        - cat
        tty: true
        env:
        - name: BUILD_SUB_PATH
          value: ${env.JOB_NAME}
        - name: NODE_OPTIONS 
          value: --max-old-space-size=5120 
        volumeMounts:
          - name: build-cache
            mountPath: /root/.m2
            subPathExpr: \$(BUILD_SUB_PATH)/.m2
          - name: up-pvc
            mountPath: /home/jenkins/agent/workspace/${env.JOB_NAME}/up
     """.stripIndent()
  }
 }*/


String getVolumes(Map config = [:]) {
  StringBuilder podVolumes = StringBuilder.newInstance()

    /*podVolumes << StringExtensions.indent('''\
      - name: build-cache
        persistentVolumeClaim:
          claimName: build-cache
    '''.stripIndent(), 2)*/

     podVolumes << StringExtensions.indent('''\
      - name: sd-upload
        persistentVolumeClaim:
          claimName: build-cache 
    '''.stripIndent(), 2)

    podVolumes << StringExtensions.indent('''\
      - name: docker-config
        projected:
          sources:
          - secret:
              name: docker-registry
              items:
                - key: .dockerconfigjson
                  path: config.json
    '''.stripIndent(), 2)
  
  if (podVolumes.size() > 0) {
    podVolumes.insert(0, '''\
      volumes:
    '''.stripIndent())
  } 
    return podVolumes.toString()
}

String getServiceAccount(Map config = [:]) {
  def Component = config.ComponentName
  def input = "${params.Region}" + '-' + "${params.Envir}" + '-' + "${Component}"
  def result = ''
  switch (input) {
      case 'EU-QA-SD':
          result = 'sd-eu-test-sagemaker'
          break
      case 'EU-PROD-SD':
          result = 'sd-eu-prod-sagemaker'
          break
      case 'NA-DEV-SD':
          result = 'sd-na-dev-sagemaker'
          break
      case 'NA-QA-SD':
          result = 'sd-na-test-sagemaker'
          break
      case 'NA-PROD-SD':
          result = 'sd-na-prod-sagemaker'
          break
      case 'EU-QA-INT':
          result = 'int-eu-test-sagemaker'
          break
      case 'EU-PROD-INT':
          result = 'int-eu-prod-sagemaker'
          break
      case 'NA-DEV-INT':
          result = 'int-na-dev-sagemaker'
          break
      case 'NA-QA-INT':
          result = 'int-na-test-sagemaker'
          break
      case 'NA-PROD-INT':
          result = 'int-na-prod-sagemaker'
          break
  }
  return result
}

String getVolumeMounts(Map config = [:]) {
  StringBuilder podVolumeMounts = StringBuilder.newInstance()

    /*podVolumeMounts << StringExtensions.indent('''\
      - name: build-cache
        mountPath: /cache
    '''.stripIndent(), 2)*/

    podVolumeMounts << StringExtensions.indent('''\
      - name: sd-upload
        mountPath: /cache
    '''.stripIndent(), 2)

    podVolumeMounts << StringExtensions.indent('''\
      - name: docker-config
        mountPath: /kaniko/.docker
    '''.stripIndent(), 2)
  
  if (podVolumeMounts.size() > 0) {
    podVolumeMounts.insert(0, '''\
      volumeMounts:
    '''.stripIndent())
  } 
    return podVolumeMounts.toString()
}