def call(body) {
    def input = "${params.Region}" + '-' + "${params.Envir}"
    def result = ''
    switch (input) {
        case 'EU-QA':
            result = 'aws eks update-kubeconfig --region eu-central-1 --name eks-test'
            break
        case 'EU-PROD':
            result = 'aws eks update-kubeconfig --region eu-central-1 --name eks-production'
            break
        case 'NA-DEV':
            result = 'aws eks update-kubeconfig --region us-east-1 --name eks-sandpit'
            break
        case 'NA-QA':
            result = 'aws eks update-kubeconfig --region us-east-1 --name eks-test-us-east-1'
            break
        case 'NA-PROD':
            result = 'aws eks update-kubeconfig --region us-east-1 --name eks-production-us-east-1'
            break
    }
    return result
  }