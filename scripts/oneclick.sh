#!/bin/bash -xe


profile=$1

script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
. "$script_dir"/common.sh #use quote here to compliant with space in dir

rm -rf $project_home/build

. "$script_dir"/build.sh

version=$(cat build/version)

. "$script_dir"/build-image.sh

. "$script_dir"/build-test-image.sh

. "$script_dir"/deploy.sh $profile $version

sleep 10
. "$script_dir"/smoke.sh $profile $version
