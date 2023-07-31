def call(body) {
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
      case 'EU-QA-SD':
          result = 'intellimap-eu-test-sagemaker'
          break
      case 'EU-PROD-SD':
          result = 'intellimap-eu-prod-sagemaker'
          break
      case 'NA-DEV-SD':
          result = 'intellimap-na-dev-sagemaker'
          break
      case 'NA-QA-SD':
          result = 'intellimap-na-test-sagemaker'
          break
      case 'NA-PROD-SD':
          result = 'intellimap-na-prod-sagemaker'
          break
  }
  return result
}