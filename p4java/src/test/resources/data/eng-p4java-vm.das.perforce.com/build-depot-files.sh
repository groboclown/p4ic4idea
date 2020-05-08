#!/bin/sh

if [ "$1" = "-h" ] || [ "$1" = "--help" ] ; then
  echo "Example for creating the checkpoint.gz and depot.tar.gz files."
  echo "YMMV"
  exit 0
fi

current_uid=$( cat /etc/passwd | grep "${USER}" | cut -f 3 -d : )

output_dir="/tmp/p4d.$$.$RANDOM"

basen="d-$$"
clean_up() {
  docker kill "x-p4d-${basen}"
  docker rm "x-p4d-${basen}"
  docker rmi "p4d-${basen}"
  docker rm "x-p4client-${basen}"
  docker rmi "p4client-${basen}"
  docker network rm "p4-net-${basen}"
  rm -rf "${output_dir}"
  exit "$1"
}
trap clean_up HUP INT TERM

mkdir -p "$output_dir" || clean_up 1

cd $( dirname "$0" ) || clean_up 1

# prod...
checkpoint_dir=$( pwd )
# testing...
# checkpoint_dir=$( pwd )/tmp-depot
# mkdir -p "${checkpoint_dir}"

cd construction || clean_up 1

( cd p4d-docker && docker build -t "p4d-${basen}" . ) || clean_up 1
( cd p4-client-docker && docker build -t "p4client-${basen}" . ) || clean_up 1

docker network create "p4-net-${basen}" || clean_up 1

# Start the Perforce server as a daemon process ("-d").  This will stay running in the background.
docker run --rm \
  --name "x-p4d-${basen}" \
  --network "p4-net-${basen}" \
  --network-alias "perforce.local" \
  -u "${current_uid}" \
  -v "${output_dir}:/opt/p4d-base" \
  -d \
  "p4d-${basen}" || clean_up 1

# Execute the Perforce construction tool.
docker run --rm \
  --name "x-p4clinet-${basen}" \
  --network "p4-net-${basen}" \
  "p4client-${basen}" || clean_up 1

test -f "${output_dir}/checkpoint.1" || clean_up 1
mv "${output_dir}/checkpoint.1" "${checkpoint_dir}/checkpoint" || clean_up 1
test -f "${checkpoint_dir}/checkpoint.gz" && rm -f "${checkpoint_dir}/checkpoint.gz"
( cd "${checkpoint_dir}" && gzip -9 checkpoint)

test -d "${output_dir}/depot" || clean_up 1
test -d "${output_dir}/p4java_stream" || clean_up 1
test -f "${checkpoint_dir}/depot.tar.gz" && rm -f "${checkpoint_dir}/depot.tar.gz"
( cd "${output_dir}" && tar zcf "${checkpoint_dir}/depot.tar.gz" depot p4java_stream )

chmod +w "${checkpoint_dir}/checkpoint.gz" "${checkpoint_dir}/depot.tar.gz"
