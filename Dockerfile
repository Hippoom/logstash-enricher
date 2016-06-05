FROM java:8

## image for test

ENV TEST_HOME /project

COPY gradlew build.gradle settings.gradle $TEST_HOME/
COPY gradle $TEST_HOME/gradle
COPY build/classes $TEST_HOME/build/classes
COPY build/resources $TEST_HOME/build/resources

# Define mountable directories
VOLUME $TEST_HOME/config
VOLUME $TEST_HOME/logs
VOLUME $TEST_HOME/build/reports

WORKDIR $TEST_HOME

ENTRYPOINT ["./gradlew"]




