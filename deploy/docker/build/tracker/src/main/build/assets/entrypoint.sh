#!/bin/bash
set -eux

########################################################################################################################

my_bind="${REPOSITORY_SEARCH_ELASTIC_TRACKER_BIND:-"0.0.0.0"}"

repository_search_elastic_index_host="${REPOSITORY_SEARCH_ELASTIC_INDEX_HOST:-repository-search-elastic-index}"
repository_search_elastic_index_port="${REPOSITORY_SEARCH_ELASTIC_INDEX_PORT:-9200}"

repository_search_elastic_index_shards="${REPOSITORY_SEARCH_ELASTIC_INDEX_SHARDS:-1}"
repository_search_elastic_index_replicas="${REPOSITORY_SEARCH_ELASTIC_INDEX_REPLICAS:-1}"

repository_service_host="${REPOSITORY_SERVICE_HOST:-repository-service}"
repository_service_port="${REPOSITORY_SERVICE_PORT:-8080}"

repository_service_admin_pass="${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"

########################################################################################################################

touch application.properties

sed -i -r 's|^[#]*\s*alfresco\.host=.*|alfresco.host='"${repository_service_host}"'|' "application.properties"
grep -q '^[#]*\s*alfresco\.host=' "application.properties" || echo "alfresco.host=${repository_service_host}" >>"application.properties"

sed -i -r 's|^[#]*\s*alfresco\.port=.*|alfresco.port='"${repository_service_port}"'|' "application.properties"
grep -q '^[#]*\s*alfresco\.port=' "application.properties" || echo "alfresco.port=${repository_service_port}" >>"application.properties"

sed -i -r 's|^[#]*\s*alfresco\.password=.*|alfresco.password='"${repository_service_admin_pass}"'|' "application.properties"
grep -q '^[#]*\s*alfresco\.password=' "application.properties" || echo "alfresco.password=${repository_service_admin_pass}" >>"application.properties"

sed -i -r 's|^[#]*\s*elastic\.host=.*|elastic.host='"${repository_search_elastic_index_host}"'|' "application.properties"
grep -q '^[#]*\s*elastic\.host=' "application.properties" || echo "elastic.host=${repository_search_elastic_index_host}" >>"application.properties"

sed -i -r 's|^[#]*\s*elastic.\index\.number_of_replicas=.*|elastic.index.number_of_replicas='"${repository_search_elastic_index_replicas}"'|' "application.properties"
grep -q '^[#]*\s*elastic.\index\.number_of_replicas=' "application.properties" || echo "elastic.index.number_of_replicas=${repository_search_elastic_index_replicas}" >>"application.properties"

sed -i -r 's|^[#]*\s*elastic\.index.\number_of_shards=.*|elastic.index.number_of_shards='"${repository_search_elastic_index_shards}"'|' "application.properties"
grep -q '^[#]*\s*elastic\.index.\number_of_shards=' "application.properties" || echo "elastic.index.number_of_shards=${repository_search_elastic_index_shards}" >>"application.properties"

sed -i -r 's|^[#]*\s*elastic\.port=.*|elastic.port='"${repository_search_elastic_index_port}"'|' "application.properties"
grep -q '^[#]*\s*elastic\.port=' "application.properties" || echo "elastic.port=${repository_search_elastic_index_port}" >>"application.properties"

sed -i -r 's|^[#]*\s*server\.address=.*|server.address='"${my_bind}"'|' "application.properties"
grep -q '^[#]*\s*server\.address=' "application.properties" || echo "server.address=${my_bind}" >>"application.properties"

sed -i -r 's|^[#]*\s*management\.server\.address=.*|management.server.address='"${my_bind}"'|' "application.properties"
grep -q '^[#]*\s*management\.server\.address=' "application.properties" || echo "management.server.address=${my_bind}" >>"application.properties"

########################################################################################################################

exec "java" "-jar" "edu_sharing-community-repository-plugin-elastic-tracker-${org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:jar.version}.jar" "${JAVA_OPTS:-}" "$@"
