def error = { String message -> throw new IllegalArgumentException(message) }

String askMandatory = { String propertyName, String defaultValue ->
    String message = "Define ${transformText(propertyName, from: NameType.PROPERTY, to: NameType.NATURAL)}"
    if (defaultValue != null) {
        message += " [${defaultValue}]"
    }
    message += ':'

    String answer = ask(message, defaultValue, propertyName)
    if (answer == null) {
        error "Not null value is required for property named ${propertyName}"
    }
    return answer
}

String askMandatory = { String propertyName -> askMandatory(propertyName, null) }


def props = [:]


props.serviceName = askMandatory 'serviceName'
props.serviceDescription = askMandatory 'serviceDescription'

props.githubOrganization = askMandatory 'githubOrganization'
props.githubRepository = askMandatory 'githubRepository', props.serviceName

props.dockerhubOrganization = askMandatory 'dockerhubOrganization', props.githubOrganization
props.dockerhubRepository = askMandatory 'dockerhubRepository', props.githubRepository

props.rootPackage = askMandatory 'rootPackage', "com.github.${transformText(props.githubOrganization, from: NameType.HYPHENATED, to: NameType.PROPERTY)}.${transformText(props.githubRepository, from: NameType.HYPHENATED, to: NameType.PROPERTY)}"

props.dockerContainerName = askMandatory 'dockerContainerName', props.serviceName.replace('-', '_')

props.externalPort = ask 'externalPort', 'no'


def templates = [
  'README.adoc',
  '.travis.yml',
  'build.gradle',
  'Dockerfile',
  'src/main/resources/logback.groovy',
  'src/docs/howto-start-docker.adoc'
]

templates.each { processTemplates it, props }

def rootPackageDir = new File(projectDir, "src/main/groovy/${rootPackage}")
if (!rootPackageDir.mkdirs()) {
  error "Can't create directory ${rootPackageDir.absolutePath}"
}

new File(rootPackageDir, 'Application.groovy').text = """
package ${rootPackage}

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
