import uk.co.cacoethes.util.NameType
import org.ini4j.Wini


def error = { String message -> throw new IllegalArgumentException(message) }

def askParameter = { boolean isOptional, String propertyName, String defaultValue = null ->
    String message = "Define ${transformText(propertyName, from: NameType.PROPERTY, to: NameType.NATURAL)}"
    if (defaultValue != null) {
        message += " [${defaultValue}]"
    }
    message += ': '

    String answer = ask(message, defaultValue, propertyName)
    if (answer == null && !isOptional) {
        error "Not-null value is required for property named ${propertyName}"
    }
    return answer
}
def askMandatory = askParameter.curry(false)
def askOptional = askParameter.curry(true)

def askSecured = { String propertyName ->
    String answer = ask("Define ${transformText(propertyName, from: NameType.PROPERTY, to: NameType.NATURAL)}: ", null)
    if (answer == null) {
        error "Not-null value is required for property named ${propertyName}"
    }
    return answer
}

def runCommand = { command ->
    def process = (command).execute([], projectDir)
    def processOutput = new StringWriter()
    process.consumeProcessOutput processOutput, processOutput
    process.waitFor()
    return processOutput.toString().trim()
}


def props = [:]


def environment = [
  'HOME',
  'DOCKER_EMAIL',
  'DOCKER_USERNAME',
  'DOCKER_PASSWORD',
  'DEPLOY_TEST_USER',
  'DEPLOY_TEST_HOST',
  'DEPLOY_PROD_USER',
  'DEPLOY_PROD_HOST',
  'TRAVIS_BRANCH',
  'DOCKER_REPO',
  'TRAVIS_BUILD_NUMBER',
  'TAG',
  'TAG_TO_DEPLOY'
]
environment.each { props."$it" = '$' + it }

def gitUsername
def gitEmail
def gitConfigFile = new File(System.getProperty('user.home'), '.gitconfig')
if (gitConfigFile.exists()) {
    def gitConfig = new Wini(gitConfigFile)
    gitUsername = gitConfig.get('user', 'name')
    gitEmail = gitConfig.get('user', 'email')
}

props.serviceName = askMandatory 'serviceName', projectDir.name
props.serviceDescription = askOptional 'serviceDescription'
if (props.serviceDescription != null && !props.serviceDescription.endsWith('.')) props.serviceDescription += '.'

props.githubOrganization = askMandatory 'githubOrganization', props.serviceName.substring(0, props.serviceName.indexOf('-'))
props.githubRepository = askMandatory 'githubRepository', props.serviceName

props.githubUsername = askMandatory 'githubUsername', gitUsername
props.githubEmail = askMandatory 'githubEmail', gitEmail

props.dockerhubOrganization = askMandatory 'dockerhubOrganization', props.githubOrganization
props.dockerhubRepository = askMandatory 'dockerhubRepository', props.githubRepository

props.rubyHome = askMandatory 'rubyHome', System.getenv('PATH').split(';').find({ it.toLowerCase().contains('ruby') })
dockerEmail = askMandatory 'dockerEmail', props.githubEmail
dockerUsername = askMandatory 'dockerUsername', props.githubOrganization
dockerPassword = askSecured 'dockerPassword'

deployTestUser = askMandatory 'deployTestUser', 'root'
deployTestHost = askMandatory 'deployTestHost'
deployProdUser = askMandatory 'deployProdUser', 'root'
deployProdHost = askMandatory 'deployProdHost'

props.rootPackage = askMandatory 'rootPackage', "com.github.${transformText(props.githubOrganization, from: NameType.HYPHENATED, to: NameType.PROPERTY)}.${transformText(props.githubRepository, from: NameType.HYPHENATED, to: NameType.PROPERTY)}"

props.dockerContainerName = askMandatory 'dockerContainerName', props.serviceName

props.externalPort = askMandatory 'externalPort', 'no'


def templates = [
        '.gitignore.gtpl',
        'README.adoc.gtpl',
        '.travis.yml',
        'build.gradle',
        'Dockerfile',
        'src/main/groovy/Application.groovy.gtpl',
        'src/main/resources/logback.groovy',
        'src/main/scripts/deploy.sh',
        'src/docs/configuration-parameters.adoc',
        'src/docs/environments.adoc'
]
templates.each { processTemplates it, props }


def rootPackageDir = new File(projectDir, "src/main/groovy/${props.rootPackage.replace('.', '/')}")
rootPackageDir.deleteDir()
if (!rootPackageDir.mkdirs()) {
  error "Can't create directory ${rootPackageDir.absolutePath}"
}
new File(projectDir, 'src/main/groovy/Application.groovy.gtpl').renameTo(new File(rootPackageDir, 'Application.groovy'))


new File(projectDir, '.gitignore.gtpl').renameTo(new File(projectDir, '.gitignore'))
new File(projectDir, 'README.adoc').delete()
new File(projectDir, 'README.adoc.gtpl').renameTo(new File(projectDir, 'README.adoc'))


[ 'controller', 'service' ].each { new File(rootPackageDir, it).mkdirs() }

runCommand([ 'git', 'init' ])
runCommand([ 'git', 'config', 'user.name', props.githubUsername ])
runCommand([ 'git', 'config', 'user.email', props.githubEmail ])

dockerEmail = runCommand([ 'ruby', "${props.rubyHome}/travis", 'encrypt', "DOCKER_EMAIL=${dockerEmail}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
dockerEmail = dockerEmail.substring(1, dockerEmail.length() - 1)

dockerUsername = runCommand([ 'ruby', "${props.rubyHome}/travis", 'encrypt', "DOCKER_USERNAME=${dockerUsername}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
dockerUsername = dockerUsername.substring(1, dockerUsername.length() - 1)

dockerPassword = runCommand([ 'ruby', "${props.rubyHome}/travis", 'encrypt', "DOCKER_PASSWORD=${dockerPassword}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
dockerPassword = dockerPassword.substring(1, dockerPassword.length() - 1)

deployTestUser = runCommand([ 'ruby', "${props.rubyHome}/travis", 'encrypt', "DEPLOY_TEST_USER=${deployTestUser}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
deployTestUser = deployTestUser.substring(1, deployTestUser.length() - 1)

deployTestHost = runCommand([ 'ruby', "${props.rubyHome}/travis", 'encrypt', "DEPLOY_TEST_HOST=${deployTestHost}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
deployTestHost = deployTestHost.substring(1, deployTestHost.length() - 1)

deployProdUser = runCommand([ 'ruby', "${props.rubyHome}/travis", 'encrypt', "DEPLOY_PROD_USER=${deployProdUser}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
deployProdUser = deployProdUser.substring(1, deployProdUser.length() - 1)

deployProdHost = runCommand([ 'ruby', "${props.rubyHome}/travis", 'encrypt', "DEPLOY_PROD_HOST=${deployProdHost}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
deployProdHost = deployProdHost.substring(1, deployProdHost.length() - 1)


new File(projectDir, '.travis.yml').text = new File(projectDir, '.travis.yml').text
                                  .replace('- secure: $DOCKER_EMAIL', "- secure: ${dockerEmail}")
                                  .replace('- secure: $DOCKER_USERNAME', "- secure: ${dockerUsername}")
                                  .replace('- secure: $DOCKER_PASSWORD', "- secure: ${dockerPassword}")
                                  .replace('- secure: $DEPLOY_TEST_USER', "- secure: ${deployTestUser}")
                                  .replace('- secure: $DEPLOY_TEST_HOST', "- secure: ${deployTestHost}")
                                  .replace('- secure: $DEPLOY_PROD_USER', "- secure: ${deployProdUser}")
                                  .replace('- secure: $DEPLOY_PROD_HOST', "- secure: ${deployProdHost}")

runCommand([ 'git', 'add', '.' ])
runCommand([ 'git', 'reset', 'lazybones.groovy' ])
runCommand([ 'git', 'commit', '-m', 'generated by `lazybones create groovy-service`' ])
runCommand([ 'git', 'remote', 'add', 'origin', "https://github.com/${props.githubOrganization}/${props.dockerhubRepository}.git" ])
