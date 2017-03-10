groovy-service
--------------

Template for web service based on Groovy and Spring Boot.

First, do the following:

Create new GitHub repository for your service at https://github.com/new

Enable Travis-CI job for it at https://travis-ci.org/profile

Create DockerHub repository for it at https://hub.docker.com/add/repository

Then you can run following command:

    lazybones create groovy-service my-new-groovy-service

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

At the end, finalize project creation with following:

Set up git-flow for your repository.

Commit .lazybones/ directory to store create configuration.

Push code to GitHub.
