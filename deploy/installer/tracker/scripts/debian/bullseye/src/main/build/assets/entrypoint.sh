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

pushd repository/elastic/tracker
exec "java" "-jar" "edu_sharing-community-repository-plugin-elastic-tracker-${org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:jar.version}.jar"