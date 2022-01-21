#!/bin/bash
set -e
set -o pipefail

########################################################################################################################

repository_search_elastic_host="${REPOSITORY_SEARCH_ELASTIC_HOST:-"127.0.0.1"}"
repository_search_elastic_port="${REPOSITORY_SEARCH_ELASTIC_PORT:-9200}"
repository_search_elastic_base="http://${repository_search_elastic_host}:${repository_search_elastic_port}"

export repository_search_elastic_host;
export repository_search_elastic_port;
export repository_search_elastic_base;

########################################################################################################################
