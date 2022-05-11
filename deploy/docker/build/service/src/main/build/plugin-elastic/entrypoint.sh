#!/bin/bash
set -eux

repository_search_elastic_index_host="${REPOSITORY_SEARCH_ELASTIC_INDEX_HOST:-repository-search-elastic-index}"
repository_search_elastic_index_port="${REPOSITORY_SEARCH_ELASTIC_INDEX_PORT:-9200}"
repository_search_elastic_index_base="http://${repository_search_elastic_index_host}:${repository_search_elastic_index_port}"

eduSConf="tomcat/shared/classes/config/cluster/edu-sharing.deployment.conf"
homeProp="tomcat/shared/classes/config/cluster/applications/homeApplication.properties.xml"

### Wait ###############################################################################################################

until wait-for-it "${repository_search_elastic_index_host}:${repository_search_elastic_index_port}" -t 3; do sleep 1; done

until [[ $(curl -sSf -w "%{http_code}\n" -o /dev/null "${repository_search_elastic_index_base}/_cluster/health?wait_for_status=yellow&timeout=3s") -eq 200 ]]; do
	echo >&2 "Waiting for ${repository_search_elastic_index_host} ..."
	sleep 3
done

### Alfresco platform ##################################################################################################

### edu-sharing platform ###############################################################################################

xmlstarlet ed -L \
	-d '/properties/entry[@key="remote_provider"]' \
	-s '/properties' -t elem -n "entry" -v "org.edu_sharing.service.provider.ElasticSearchProvider" \
	--var entry '$prev' \
	-i '$entry' -t attr -n "key" -v "remote_provider" \
	"${homeProp}"

hocon -f "${eduSConf}" \
	set "elasticsearch.servers" '["'"${repository_search_elastic_index_host}:${repository_search_elastic_index_port}"'"]'
