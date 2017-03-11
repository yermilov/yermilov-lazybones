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

def runCommand = { def command ->
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
  'TRAVIS_BRANCH',
  'DOCKER_REPO',
  'TRAVIS_BUILD_NUMBER',
  'TAG'
]
environment.each { props."$it" = '$' + it }

def gitUsername
def gitEmail
def gitConfigFile = new File(System.getProperty("user.home"), ".gitconfig")
if (gitConfigFile.exists()) {
    def gitConfig = new Wini(gitConfigFile)
    gitUsername = gitConfig.get("user", "name")
    gitEmail = gitConfig.get("user", "email")
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

props.rootPackage = askMandatory 'rootPackage', "com.github.${transformText(props.githubOrganization, from: NameType.HYPHENATED, to: NameType.PROPERTY)}.${transformText(props.githubRepository, from: NameType.HYPHENATED, to: NameType.PROPERTY)}"

props.dockerContainerName = askMandatory 'dockerContainerName', props.serviceName.replace('-', '_')

props.externalPort = askMandatory 'externalPort', 'no'


def templates = [
        '.gitignore.gtpl',
        'README.adoc.gtpl',
        '.travis.yml',
        'build.gradle',
        'Dockerfile',
        'src/main/resources/logback.groovy',
        'src/docs/howto-start-docker.adoc'
]
templates.each { processTemplates it, props }


def rootPackageDir = new File(projectDir, "src/main/groovy/${props.rootPackage.replace('.', '/')}")
rootPackageDir.deleteDir()
if (!rootPackageDir.mkdirs()) {
  error "Can't create directory ${rootPackageDir.absolutePath}"
}

new File(rootPackageDir, 'Application.groovy').text = """
package ${props.rootPackage}

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class Application {

    static void main(String[] args) {
        SpringApplication.run(Application, args)
    }
}
""".trim()


[ 'controller', 'service' ].each { new File(rootPackageDir, it).mkdirs() }

runCommand([ "git", "init" ])
runCommand([ "git", "config", "user.name", props.githubUsername ])
runCommand([ "git", "config", "user.email", props.githubEmail ])

dockerEmail = runCommand([ "ruby", "${props.rubyHome}/travis", "encrypt", "DOCKER_EMAIL=${dockerEmail}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
dockerEmail = dockerEmail.substring(1, dockerEmail.length() - 1)

dockerUsername = runCommand([ "ruby", "${props.rubyHome}/travis", "encrypt", "DOCKER_USERNAME=${dockerUsername}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
dockerUsername = dockerUsername.substring(1, dockerUsername.length() - 1)

dockerPassword = runCommand([ "ruby", "${props.rubyHome}/travis", "encrypt", "DOCKER_PASSWORD=${dockerPassword}", "--repo=${props.githubOrganization}/${props.githubRepository}" ])
dockerPassword = dockerPassword.substring(1, dockerPassword.length() - 1)

new File(projectDir, ".travis.yml").text = new File(projectDir, ".travis.yml").text
                                  .replace('- secure: $DOCKER_EMAIL', "- secure: ${dockerEmail}")
                                  .replace('- secure: $DOCKER_USERNAME', "- secure: ${dockerUsername}")
                                  .replace('- secure: $DOCKER_PASSWORD', "- secure: ${dockerPassword}")

runCommand([ "git", "add", "." ])
runCommand([ "git", "reset", "lazybones.groovy" ])
runCommand([ "git", "commit", "-m", "generated by `lazybones create groovy-service`" ])
runCommand([ "git", "remote", "add", "origin", "https://github.com/${props.githubOrganization}/${props.dockerhubRepository}.git" ])
