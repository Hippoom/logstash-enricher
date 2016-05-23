FROM java:8-jdk-alpine

COPY build/version build/libs/*.jar /opt/scaleworks/graph/

# Define mountable directories
VOLUME /opt/scaleworks/graph/config
VOLUME /opt/scaleworks/graph/logs

WORKDIR /opt/scaleworks/graph

CMD java -jar scaleworks-graph.jar

EXPOSE 8080



