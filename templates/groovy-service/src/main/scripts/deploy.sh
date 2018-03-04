#!/usr/bin/env sh

##############################################################################
##
##  Stop and kill currently running docker image, pull newest version and
##  run it.
##
##############################################################################

warn ( ) {
    echo "__DOLLAR_STAR__"
}

warn "Currently running docker images"
docker ps -a

warn "Killing currently running docker image..."
docker kill ${dockerContainerName}; docker rm ${dockerContainerName}

warn "Pulling latest docker image..."
docker pull ${dockerhubOrganization}/${dockerhubRepository}:$TAG_TO_DEPLOY

warn "Starting docker image..."
docker run -dit --name ${dockerContainerName} -e LOG_PATH=/mnt/logs -v /mnt/logs:/mnt/logs -e LOGZIO_TOKEN=$LOGZIO_TOKEN ${dockerhubOrganization}/${dockerhubRepository}:$TAG_TO_DEPLOY

warn "Currently running docker images"
docker ps -a
