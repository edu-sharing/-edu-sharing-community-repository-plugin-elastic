#!/bin/bash
set -e
set -o pipefail

########################################################################################################################

execution_folder="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

pushd "$execution_folder" &> /dev/null
# load the default configuration
if [[ -f ".env.base" ]] ; then
	echo "Load .env.base"
	source .env.base
fi
popd

########################################################################################################################

usage() {
	echo "Options:"
	echo ""

	echo "-?"
	echo "--help"
	echo "  Display available options"
	echo ""

	echo "-f = environment file"
	echo "--file"
	echo "  Loads the configuration from the specified environment file"
	echo ""

	echo "--local"
	echo "  Use local maven cache for installation"
}

########################################################################################################################

use_local_maven_cache=0
while true; do
	flag="$1"
	shift || break

	case "$flag" in
			--help|'-?') usage && exit 0 ;;
			--file|-f) source "$1" && shift	;;
			--local) use_local_maven_cache=1 ;;
			*) {
				echo "error: unknown flag: $flag"
				usage
			}  >&2
			exit 1 ;;
	esac
done

########################################################################################################################

#
my_admin_pass="${REPOSITORY_SERVICE_ADMIN_PASS:-"admin"}"

repository_host="${REPOSITORY_SERVICE_HOST:-"127.0.0.1"}"
repository_port="${REPOSITORY_SERVICE_PORT:-8080}"

repository_search_elastic_tracker_bind="${REPOSITORY_SEARCH_ELASTIC_TRACKER_BIND:-"127.0.0.1"}"
repository_search_elastic_tracker_port="${REPOSITORY_SEARCH_ELASTIC_TRACKER_PORT:-8080}"
repository_search_elastic_tracker_management_bind="${REPOSITORY_SEARCH_ELASTIC_TRACKER_MANAGEMENT_BIND:-"127.0.0.1"}"
repository_search_elastic_tracker_management_port="${REPOSITORY_SEARCH_ELASTIC_TRACKER_MANAGEMENT_PORT:-8081}"

repository_search_elastic_index_host="${REPOSITORY_SEARCH_ELASTIC_INDEX_HOST:-"127.0.0.1"}"
repository_search_elastic_index_port="${REPOSITORY_SEARCH_ELASTIC_INDEX_PORT:-9200}"
#repository_search_elastic_index_base="http://${repository_search_elastic_index_host}:${repository_search_elastic_index_port}"
repository_search_elastic_index_shards="${REPOSITORY_SEARCH_ELASTIC_INDEX_SHARDS:-1}"
repository_search_elastic_index_replicas="${REPOSITORY_SEARCH_ELASTIC_INDEX_REPLICAS:-1}"

########################################################################################################################

info() {
  echo "#########################################################################"
  echo ""
  echo "elastic index:"
  echo ""
  echo "  Host:                ${repository_search_elastic_index_host}"
  echo "  Port:                ${repository_search_elastic_index_port}"
  echo ""
  echo "  Shards:              ${repository_search_elastic_index_shards}"
  echo "  Replicas:            ${repository_search_elastic_index_replicas}"
  echo ""
  echo "elastic tracker:"
  echo ""
  echo "  Bind:                ${repository_search_elastic_tracker_bind}"
  echo "  Port:                ${repository_search_elastic_tracker_port}"
  echo ""
  echo "  Management:"
  echo ""
  echo "    Bind:              ${repository_search_elastic_tracker_management_bind}"
  echo "    Port:              ${repository_search_elastic_tracker_management_port}"
  echo ""
}

########################################################################################################################

#if [[ -f "/etc/systemd/system/elastictracker" && "$(systemctl status elastictracker)" == "elastictracker is running"  ]] ; then
if [[ -f "/etc/systemd/system/elastictracker" && $(systemctl is-active --quiet elastictracker) ]] ; then
	echo ""
	echo "You must stop the elastictracker before you can run the installation."
	exit 1
fi

########################################################################################################################

[[ -d /opt/edu-sharing/repository/elastic/tracker ]] && {
	echo "- remove repository plugin elastic tracker"
	rm -rf /opt/edu-sharing/repository/elastic/tracker
	mkdir -p /opt/edu-sharing/repository/elastic/tracker
}

### elastic tracker - fix security issues ##############################################################################

echo "- create worker user"
id -u worker &>/dev/null || adduser --home=/opt/edu-sharing/repository/elastic/tracker --disabled-password --gecos "" --shell=/bin/bash worker


### elastic tracker - download #########################################################################################

if [[ use_local_maven_cache -eq 1 ]] ; then
	echo "- WARNING local maven cache is used"
else
	echo "- download repository plugin elastic tracker"
	mvn -q dependency:get \
		-Dartifact=org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:${org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:jar.version} \
		-DremoteRepositories=edusharing-remote::::https://artifacts.edu-sharing.com/repository/maven-remote/ \
		-Dtransitive=false
fi

echo "- unpack repository plugin elastic tracker"
mvn -q dependency:copy \
	-Dartifact=org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:${org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:jar.version} \
	-DoutputDirectory=/opt/edu-sharing/repository/elastic/tracker

chown -RL worker:worker /opt/edu-sharing/repository/elastic/tracker

###  elastic tracker ###################################################################################################

echo "- update repository plugin elastic tracker"

props="/opt/edu-sharing/repository/elastic/tracker/application.properties"
touch "${props}"

sed -i -r 's|^[#]*\s*server\.address=.*|server.address='"${repository_search_elastic_tracker_bind}"'|' "${props}"
grep -q '^[#]*\s*server\.address=' "${props}" || echo "server.address=${repository_search_elastic_tracker_bind}" >>"${props}"

sed -i -r 's|^[#]*\s*server\.port=.*|server.port='"${repository_search_elastic_tracker_port}"'|' "${props}"
grep -q '^[#]*\s*server\.port=' "${props}" || echo "server.port=${repository_search_elastic_tracker_port}" >>"${props}"

sed -i -r 's|^[#]*\s*management\.server.\address=.*|management.server.address='"${repository_search_elastic_tracker_management_bind}"'|' "${props}"
grep -q '^[#]*\s*management\.server.\address=' "${props}" || echo "management.server.address=${repository_search_elastic_tracker_management_bind}" >>"${props}"

sed -i -r 's|^[#]*\s*management\.server.\port=.*|management.server.port='"${repository_search_elastic_tracker_management_port}"'|' "${props}"
grep -q '^[#]*\s*management\.server.\port=' "${props}" || echo "management.server.port=${repository_search_elastic_tracker_management_port}" >>"${props}"

sed -i -r 's|^[#]*\s*alfresco\.host=.*|alfresco.host='"${repository_host}"'|' "${props}"
grep -q '^[#]*\s*alfresco\.host=' "${props}" || echo "alfresco.host=${repository_host}" >>"${props}"

sed -i -r 's|^[#]*\s*alfresco\.port=.*|alfresco.port='"${repository_port}"'|' "${props}"
grep -q '^[#]*\s*alfresco\.port=' "${props}"|| echo "alfresco.port=${repository_port}" >>"${props}"

sed -i -r 's|^[#]*\s*alfresco\.password=.*|alfresco.password='"${my_admin_pass}"'|' "${props}"
grep -q '^[#]*\s*alfresco\.password=' "${props}" || echo "alfresco.password=${my_admin_pass}" >>"${props}"

sed -i -r 's|^[#]*\s*elastic\.host=.*|elastic.host='"${repository_search_elastic_index_host}"'|' "${props}"
grep -q '^[#]*\s*elastic\.host=' "${props}" || echo "elastic.host=${repository_search_elastic_index_host}" >>"${props}"

sed -i -r 's|^[#]*\s*elastic\.port=.*|elastic.port='"${repository_search_elastic_index_port}"'|' "${props}"
grep -q '^[#]*\s*elastic\.port=' "${props}" || echo "elastic.port=${repository_search_elastic_index_port}" >>"${props}"

sed -i -r 's|^[#]*\s*elastic\.index.\number_of_shards=.*|elastic.index.number_of_shards='"${repository_search_elastic_index_shards}"'|' "${props}"
 grep -q '^[#]*\s*elastic\.index.\number_of_shards=' "${props}" || echo "elastic.index.number_of_shards=${repository_search_elastic_index_shards}" >>"${props}"

sed -i -r 's|^[#]*\s*elastic.\index\.number_of_replicas=.*|elastic.index.number_of_replicas='"${repository_search_elastic_index_replicas}"'|' "${props}"
grep -q '^[#]*\s*elastic.\index\.number_of_replicas=' "${props}" || echo "elastic.index.number_of_replicas=${repository_search_elastic_index_replicas}" >>"${props}"

### elastic tracker - register systemd service #########################################################################

pushd /etc/systemd/system

elastic_tracker_jar=edu_sharing-community-repository-plugin-elastic-tracker-${org.edu_sharing:edu_sharing-community-repository-plugin-elastic-tracker:jar.version}.jar

if [[ ! -f elastictracker.service ]]; then
	echo "- create systemd service"
	touch elastictracker.service
	{
		echo "[Unit]"
		echo "Description=edu-sharing repository plugin elastic tracker"
		echo "After=syslog.target network.target elasticsearch.service"
		echo ""
		echo "[Service]"
		echo "WorkingDirectory=/opt/edu-sharing/repository/elastic/tracker"
		echo "User=worker"
		echo "ExecStart=/usr/bin/java -jar /opt/edu-sharing/repository/elastic/tracker/${elastic_tracker_jar}"
		echo "SuccessExitStatus=143"
		echo ""
		echo "[Install]"
		echo "WantedBy=multi-user.target"
	 } >> elastictracker.service
else
	echo "- update systemd service"

	sed -i -r 's|^WorkingDirectory=.*|WorkingDirectory='"/opt/edu-sharing/repository/elastic/tracker"'|' elastictracker.service
   grep -q '^WorkingDirectory=' elastictracker.service || echo "WorkingDirectory=/opt/edu-sharing/repository/elastic/tracker" >> elastictracker.service

	sed -i -r 's|^ExecStart=.*|ExecStart='"/opt/edu-sharing/repository/elastic/tracker/${elastic_tracker_jar}"'|' elastictracker.service
   grep -q '^ExecStart=' elastictracker.service || echo "ExecStart=/usr/bin/java -jar /opt/edu-sharing/repository/elastic/tracker/${elastic_tracker_jar}" >> elastictracker.service
fi

popd

info >> "$execution_folder/install_log-$(date "+%Y.%m.%d-%H.%M.%S").txt"
info

echo "- done."
exit

########################################################################################################################
