FROM java:8-jdk-alpine

# the build space is under "build"

ENV APP_HOME /opt/scaleworks/graph
ENV APP_JAR scaleworks-graph.jar

COPY version libs/*.jar $APP_HOME/
COPY resources/main/docker-entry-point.sh /

RUN chmod +x /docker-entry-point.sh

# Define mountable directories
VOLUME $APP_HOME/config
VOLUME $APP_HOME/logs

WORKDIR $APP_HOME

EXPOSE 8080

ENTRYPOINT [ "/docker-entry-point.sh" ]



