package ${rootPackage}.config

import com.mongodb.Mongo
import com.mongodb.MongoClient
import com.mongodb.MongoCredential
import com.mongodb.ServerAddress
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@EnableMongoRepositories('${rootPackage}.repository')
class MongoConfiguration extends AbstractMongoConfiguration {

    static String DATABASE_NAME = System.getenv('MONGO_DATABASE_NAME') ?: '${mongoDatabase}'
    static String AUTHENTICATION_DATABASE_NAME = System.getenv('MONGO_AUTH_DATABASE_NAME') ?: '${mongoAuthDatabase}'
    static String HOST = System.getenv('MONGO_HOST') ?: '${mongoDockerContainerName}'
    static Integer PORT = System.getenv('MONGO_PORT') ? Integer.parseInt(System.getenv('MONGO_PORT')) : ${mongoPort}
    static String USERNAME = System.getenv('MONGO_USERNAME') ?: '${serviceUsername}'
    static String PASSWORD = System.getenv('MONGO_PASSWORD')

    @Override
    protected String getDatabaseName() {
        DATABASE_NAME
    }

    @Override
    Mongo mongo() throws Exception {
        new MongoClient(new ServerAddress(HOST, PORT), [ MongoCredential.createCredential(USERNAME, AUTHENTICATION_DATABASE_NAME, PASSWORD.toCharArray()) ])
    }

    @Override
    protected String getMappingBasePackage() {
        '${rootPackage}.domain'
    }
}