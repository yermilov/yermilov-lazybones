#!/usr/bin/env sh

##############################################################################
##
##  Stop and kill currently running docker image, pull newest version and
##  run it.
##
##############################################################################

warn ( ) {
    echo "$*"
}

warn "Currently running docker images"
docker ps -a

warn "Killing currently running docker image..."
docker kill ${dockerContainerName}; docker rm ${dockerContainerName}

warn "Pulling latest docker image..."
docker pull ${dockerhubOrganization}/${dockerhubRepository}:$TAG_TO_DEPLOY

warn "Starting docker image..."
docker run -dit --name ${dockerContainerName} -e LOG_PATH=/logs -v /logs:/logs${externalPort != 'no' ? ' -p ' + externalPort + ':8080' : ''} ${dockerhubOrganization}/${dockerhubRepository}:$TAG_TO_DEPLOY

warn "Currently running docker images"
docker ps -a
