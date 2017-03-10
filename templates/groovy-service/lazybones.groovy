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

props.dockerhubOrganization = askMandatory 'dockerhubOrganization', props.githubOrganization
props.dockerhubRepository = askMandatory 'dockerhubRepository', props.githubRepository

props.rubyHome = askMandatory 'rubyHome', System.getenv('PATH').split(';').find({ it.toLowerCase().contains('ruby') })
props.dockerEmail = askMandatory 'dockerEmail', gitEmail

props.rootPackage = askMandatory 'rootPackage', "com.github.${transformText(props.githubOrganization, from: NameType.HYPHENATED, to: NameType.PROPERTY)}.${transformText(props.githubRepository, from: NameType.HYPHENATED, to: NameType.PROPERTY)}"

props.dockerContainerName = askMandatory 'dockerContainerName', props.serviceName.replace('-', '_')

props.externalPort = askMandatory 'externalPort', 'no'


def templates = [
  'README.tmpl',
  '.travis.yml',
  'build.gradle',
  'Dockerfile',
  'src/main/resources/logback.groovy',
  'src/docs/howto-start-docker.adoc'
]
templates.each { processTemplates it, props }
new File(projectDir, 'README.tmpl').renameTo(new File(projectDir, 'README.adoc'))
new File(projectDir, 'README.md').delete()


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

def encryptDockerEmailProcess = ([ "ruby", "${props.rubyHome}/travis", "encrypt", "DOCKER_EMAIL=${props.dockerEmail}" ]).execute([], projectDir)
def out = new StringWriter()
encryptDockerEmailProcess.consumeProcessOutput out, out
encryptDockerEmailProcess.waitFor()
new File(projectDir, 'travis.txt').text = out.toString()
