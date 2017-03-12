= ${serviceName}
:linkattrs:
${serviceDescription != null ? '\n' + serviceDescription + '\n': ''}
== Travis job

image:https://travis-ci.org/${githubOrganization}/${githubRepository}.svg?branch=develop["Travis job", link="https://travis-ci.org/${githubOrganization}/${githubRepository}"]

== Docker repository

https://hub.docker.com/r/${dockerhubOrganization}/${dockerhubRepository}/

== Documentation

link:src/docs/howto-start-docker.adoc[HOWTO: start service as docker container, window="_blank"]

link:src/docs/howto-run-service.adoc[HOWTO: run service, window="_blank"]