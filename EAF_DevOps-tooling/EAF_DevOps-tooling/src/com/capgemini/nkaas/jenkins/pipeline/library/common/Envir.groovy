class EnvirClass {
    def getClusterValue(input) {
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

    def getIngressValue(input) {
    def input = "${params.Region}" + '-' + "${params.Envir}"
    def result = ''
    switch (input) {
        case 'EU-QA':
            result = '.gadm.eks-test.eu.eafcore.com'
            break
        case 'EU-PROD':
            result = '.gadm.eks-production.eu.eafcore.com'
            break
        case 'NA-DEV':
            result = '.gadm.eks-sandpit.na.eafcore.com'
            break
        case 'NA-QA':
            result = '.gadm.eks-test.na.eafcore.com'
            break
        case 'NA-PROD':
            result = '.gadm.eks-production.na.eafcore.com'
            break
    }
    return result
  }

    def getServiceAccount(input) {
    def input = "${params.Region}" + '-' + "${params.Envir}"
    def result = ''
    switch (input) {
        case 'EU-QA':
            result = 'jenkins-eu-test'
            break
        case 'EU-PROD':
            result = 'jenkins-eu-prod'
            break
        case 'NA-DEV':
            result = 'jenkins-na-dev'
            break
        case 'NA-QA':
            result = 'jenkins-na-test'
            break
        case 'NA-PROD':
            result = 'jenkins-na-prod'
            break
    }
    return result
  }
}

// Example usage:
def myClass = new MultipleCaseClass()

println myClass.processValue(1)   // Output: "Value is 1"
println myClass.processValue(3)   // Output: "Value is 3 or 4"
println myClass.processValue(5)   // Output: "Unknown value"

println myClass.processLetter('a') // Output: "Letter is A or a"
println myClass.processLetter('B') // Output: "Letter is B or b"
println myClass.processLetter('z') // Output: "Unknown letter"
