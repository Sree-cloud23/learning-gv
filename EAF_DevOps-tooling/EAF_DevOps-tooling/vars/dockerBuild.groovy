/**
 * Execute the container image build using Kaniko
 */
def call(String CI_COMMIT_TAG, String CI_REGISTRY_IMAGE) {
  container(name: 'kaniko', shell: '/busybox/sh') {
    sh """#!/busybox/sh
      /kaniko/executor --dockerfile Dockerfile --context \$(pwd) \
        --snapshot-mode redo \
        --destination ${CI_REGISTRY_IMAGE}:${CI_COMMIT_TAG}
    """
  }
}
