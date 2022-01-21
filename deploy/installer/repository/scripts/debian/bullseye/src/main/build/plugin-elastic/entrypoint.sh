#!/bin/bash

repository_search_elastic_host="${REPOSITORY_SEARCH_ELASTIC_HOST:-"127.0.0.1"}"
repository_search_elastic_port="${REPOSITORY_SEARCH_ELASTIC_PORT:-9200}"
repository_search_elastic_base="http://${repository_search_elastic_host}:${repository_search_elastic_port}"

until wait-for-it "${repository_search_elastic_host}:${repository_search_elastic_port}" -t 3; do sleep 1; done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null "${repository_search_elastic_base}/_cluster/health?wait_for_status=yellow&timeout=3s") -eq 200 ]]; do
	echo >&2 "Waiting for elasticsearch ..."
	sleep 3
done