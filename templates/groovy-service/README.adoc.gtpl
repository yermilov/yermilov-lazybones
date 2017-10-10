= ${serviceName}
:linkattrs:
${serviceDescription != null ? '\n' + serviceDescription + '\n': ''}
== Travis job

image:https://travis-ci.org/${githubOrganization}/${githubRepository}.svg?branch=develop["Travis job", link="https://travis-ci.org/${githubOrganization}/${githubRepository}"]

== Code coverage

image:https://codecov.io/gh/${githubOrganization}/${githubRepository}/branch/develop/graph/badge.svg["Code coverage", link="https://codecov.io/gh/${githubOrganization}/${githubRepository}"]

== Docker repository

https://hub.docker.com/r/${dockerhubOrganization}/${dockerhubRepository}/

== Documentation

== Documentation

link:src/main/scripts/deploy.sh[HOWTO: start service as docker container, window="_blank"]

link:src/docs/configuration-parameters.adoc[Service configuration parameters]

link:src/docs/environments.adoc[Environments]
