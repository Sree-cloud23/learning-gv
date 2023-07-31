#!/usr/bin/groovy
def call(Map stageParams) {
container('node') {
     withEnv([
        'npm_config_cache=npm-cache',
        'HOME=.',
    ]) {
            sh 'npm install && npm run build-stage' 
     }
   }
}
