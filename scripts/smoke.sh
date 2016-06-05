#!/bin/bash -xe

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. "$script_dir"/common.sh #use quote here to compliant with space in dir

profile=$1
version=$2


docker run \
           --name $profile-scaleworks-graph-test \
           -t \
           -v "$user_home"/.gradle:/root/.gradle \
           -v "$project_home"/build/reports:/project/build/reports \
           --link $profile-scaleworks-graph:app \
           $test_image:$version \
           smokeTest --no-daemon
