#!/bin/bash

# this script is docker specific

systemctl start elasticsearch

./install.sh -f .env "$@" || exit 1
./install.sh -f .env "$@" || exit 1


until wait-for-it "localhost:9200" -t 3; do sleep 1; done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null "http://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=3s") -eq 200 ]]; do
	echo >&2 "Waiting for elasticsearch ..."
	sleep 3
done

until wait-for-it "${REPOSITORY_SERVICE_HOST:-127.0.0.1}:${REPOSITORY_SERIVCE_PORT:-8080}" -t 3; do sleep 1; done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "http://${REPOSITORY_SERVICE_HOST:-127.0.0.1}:${REPOSITORY_SERIVCE_PORT:-8080}/edu-sharing/rest/_about/status/SERVICE?timeoutSeconds=3") -eq 200 ]]; do
	echo >&2 "Waiting for edu-sharing ..."
	sleep 3
done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null -H 'Accept: application/json' "http://${REPOSITORY_SERVICE_HOST:-127.0.0.1}:${REPOSITORY_SERIVCE_PORT:-8080}/edu-sharing/rest/_about/status/SEARCH?timeoutSeconds=3") -eq 200 ]]; do
	echo >&2 "Waiting for edu-sharing ..."
	sleep 3
done

pushd elastictracker
exec "java" "-jar" "edu_sharing-community-repository-plugin-elastic-tracker-${org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:jar.version}.jar"