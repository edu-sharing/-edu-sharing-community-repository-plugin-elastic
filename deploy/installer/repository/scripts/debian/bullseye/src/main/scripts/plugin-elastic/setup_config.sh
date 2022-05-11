#!/bin/bash
set -e
set -o pipefail

eduSConf="tomcat/shared/classes/config/cluster/edu-sharing.deployment.conf"
homeProp="tomcat/shared/classes/config/cluster/applications/homeApplication.properties.xml"

########################################################################################################################

echo "- update edu-sharing env for plugin elastic"

pushd "$ALF_HOME" &> /dev/null

### edu-sharing ########################################################################################################

xmlstarlet ed -L \
	-d '/properties/entry[@key="remote_provider"]' \
	-s '/properties' -t elem -n "entry" -v "org.edu_sharing.service.provider.ElasticSearchProvider" \
	--var entry '$prev' \
	-i '$entry' -t attr -n "key" -v "remote_provider" \
	"${homeProp}"

hocon -f "${eduSConf}" \
	set "elasticsearch.servers" '["'"${repository_search_elastic_index_host}:${repository_search_elastic_index_port}"'"]'

popd

########################################################################################################################
