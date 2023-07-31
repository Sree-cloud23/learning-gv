def call(body) {
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