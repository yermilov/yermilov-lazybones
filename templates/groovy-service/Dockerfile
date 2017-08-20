FROM openjdk:8

RUN mkdir -p /usr/src/${serviceName} && mkdir -p /usr/app

COPY build/distributions/* /usr/src/${serviceName}/

RUN unzip /usr/src/${serviceName}/${serviceName}-*.zip -d /usr/app/ && ln -s /usr/app/${serviceName}-* /usr/app/${serviceName}

WORKDIR /usr/app/${serviceName}

EXPOSE 8080
ENV ENVIRONMENT_NAME test
ENTRYPOINT [ "sh", "-c", "./bin/${serviceName} --spring.profiles.active=$ENVIRONMENT_NAME" ]
CMD []
