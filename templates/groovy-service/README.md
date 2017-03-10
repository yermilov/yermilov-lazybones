groovy-service
--------------

Template for web service based on Groovy and Spring Boot:

After you've just run following command:

    lazybones create groovy-service my-new-groovy-service --with-git

You got following project structure:

    <project>
      |
      +- src
      |   |
      |   +- docs
      |   |   |
      |   |   +- howto-run-service.adoc
      |   |   +- howto-start-docker.adoc
      |   |
      |   +- main
      |       |
      |       +- groovy
      |       |    |
      |       |    +- $rootPackage
      |       |         |
      |       |         +- controller
      |       |         |     |
      |       |         |     ...
      |       |         +- service
      |       |         |     |
      |       |         |     ...
      |       |         +- Application.groovy
      |       +- resources
      |            |
      |            +- logback.groovy
      +- .gitignore
      +- .travis.yml
      +- build.gradle
      +- Dockerfile
      +- README.adoc

Now you should do the following:

Create new GitHub repository that match the one you already provided to lazybones configuration at  https://github.com/new

Enable Travis-CI job at https://travis-ci.org/profile

Create DockerHub repository that match the one you already provided to lazybones configuration at https://hub.docker.com/add/repository

Set up git-flow for your repository.

Add GitHub repository as origin remote to your local one.

Encode your DockerHub credentials for Travis-CI:

    travis encrypt DOCKER_EMAIL=? --add
    travis encrypt DOCKER_USERNAME=? --add
    travis encrypt $DOCKER_PASSWORD=? --add

Push code to GitHub.
