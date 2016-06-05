#!/bin/sh

set -e

if [ -z "$1" -o  "${1:0:1}" = '-' ]; then
    exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/urandom -jar ${APP_HOME}/${APP_JAR} --spring.profiles.active=${APP_PROFILE} "$@"
fi

exec "$@"