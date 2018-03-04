import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy

appender('STDOUT', ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'
    }
}

appender('FILE', RollingFileAppender) {
    file = "__GET_LOG_PATH_FROM_ENV__/${serviceName}.log"

    encoder(PatternLayoutEncoder) {
        pattern = '%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n'
    }

    rollingPolicy(TimeBasedRollingPolicy) {
        FileNamePattern = "__GET_LOG_PATH_FROM_ENV__/${serviceName}.%d{yyyy-MM-dd}.log"
    }
}

appender('LOGZIO', LogzioLogbackAppender) {
    token = System.getenv('LOGZIO_TOKEN') ?: new File('src/main/resources/logzio-dev.properties').text
    logzioUrl = 'https://listener.logz.io:8071'

    additionalFields="service=${serviceName};env=__GET_ENV_NAME__"
}

def shutdownHook() {
    def shutdownHook = new DelayingShutdownHook()
    shutdownHook.setContext(context)

    Thread hookThread = new Thread(shutdownHook, "Logback shutdown hook [__CONTEXT_NAME__]")
    context.putObject('SHUTDOWN_HOOK', hookThread)
    Runtime.getRuntime().addShutdownHook(hookThread)
}
shutdownHook()

String SERVICE_LOG_LEVEL = System.getenv('SERVICE_LOG_LEVEL') ?: (System.getenv('ENVIRONMENT_NAME') == 'prod' ? 'INFO' : 'DEBUG')
String ROOT_LOG_LEVEL = System.getenv('ROOT_LOG_LEVEL') ?: 'WARN'

root(Level.toLevel(ROOT_LOG_LEVEL), ['STDOUT', 'FILE', 'LOGZIO' ])
logger('${rootPackage}', Level.toLevel(SERVICE_LOG_LEVEL), [ 'STDOUT', 'FILE', 'LOGZIO' ], false)
