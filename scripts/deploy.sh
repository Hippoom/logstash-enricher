#!/bin/bash -xe

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. "$script_dir"/common.sh #use quote here to compliant with space in dir

profile=$1
version=$2

docker rm -f -v $profile-scaleworks-graph || true

docker run --name $profile-scaleworks-graph \
           -P \
           -d \
           -e APP_PROFILE=${profile} \
           ${main_image}:${version}

