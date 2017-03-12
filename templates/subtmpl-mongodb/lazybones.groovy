import uk.co.cacoethes.util.NameType


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


def props = parentParams


props.mongoDatabase = askMandatory 'mongoDatabase', parentParams.githubOrganization
props.mongoAuthDatabase = askMandatory 'mongoAuthDatabase', 'admin'
props.mongoDockerContainerName = askMandatory 'mongoDockerContainerName', "${props.mongoDatabase}_mongo"
props.mongoPort = askMandatory 'mongoPort', '27017'
props.serviceUsername = askMandatory 'serviceUsername', "${transformText(parentParams.serviceName, from: NameType.HYPHENATED, to: NameType.PROPERTY)}Service"
props.adminUsername = askMandatory 'adminUsername', "${props.mongoDatabase}Admin"
props.rootUsername = askMandatory 'rootUsername', "${props.mongoDatabase}Root"


def templates = [
        'src/docs/mongodb-setup.adoc',
        'src/main/groovy/MongoConfiguration.groovy.gtpl'
]
templates.each { processTemplates it, props }


new File(templateDir, 'src/docs/mongodb-setup.adoc').renameTo(new File(projectDir, 'src/docs/mongodb-setup.adoc'))


def configPackageDir = new File(projectDir, "src/main/groovy/${parentParams.rootPackage.replace('.', '/')}/config")
configPackageDir.deleteDir()
if (!configPackageDir.mkdirs()) {
    error "Can't create directory ${configPackageDir.absolutePath}"
}
new File(templateDir, 'src/main/groovy/MongoConfiguration.groovy.gtpl').renameTo(new File(configPackageDir, 'MongoConfiguration.groovy'))


def howtoStartDockerFile = new File(projectDir, 'src/docs/howto-start-docker.adoc')
howtoStartDockerFile.text = howtoStartDockerFile.text.replace('Following environment variable are available for global service configuration:', ("""
Following environment variable are available for global service configuration:

MONGO_DATABASE_NAME - mongodb database name to use (default is ${props.mongoDatabase})

MONGO_AUTH_DATABASE_NAME - mongodb database name used for authentication (default is ${props.mongoAuthDatabase})

MONGO_HOST - host of mongodb instance (default is ${props.mongoDockerContainerName})

MONGO_PORT - port of mongodb instance (default is ${props.mongoPort})

MONGO_USERNAME - username for authentication to mongodb (default is ${props.serviceUsername})

MONGO_PASSWORD - password for authentication to mongodb
""".trim()))

howtoStartDockerFile.text = howtoStartDockerFile.text.replace('-v', "--link ${props.mongoDockerContainerName} -e MONGO_PASSWORD=?MONGODB_PASSWORD -v")


def buildGradleFile = new File(projectDir, 'build.gradle')
buildGradleFile.text = buildGradleFile.text.replace('compile \'org.springframework.boot:spring-boot-starter-web\'', ("""
    compile 'org.springframework.boot:spring-boot-starter-web'
    compile 'org.springframework.data:spring-data-mongodb'
""".trim()))


def readmeFile = new File(projectDir, 'README.adoc')
readmeFile.text += '\nlink:src/docs/mongodb-setup.adoc[MongoDB setup]\n'
