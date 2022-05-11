#!/bin/bash
set -e
set -o pipefail

########################################################################################################################

repository_search_elastic_index_host="${REPOSITORY_SEARCH_ELASTIC_INDEX_HOST:-"127.0.0.1"}"
repository_search_elastic_index_port="${REPOSITORY_SEARCH_ELASTIC_INDEX_PORT:-9200}"
repository_search_elastic_index_base="http://${repository_search_elastic_index_host}:${repository_search_elastic_index_port}"

export repository_search_elastic_index_host;
export repository_search_elastic_index_port;
export repository_search_elastic_index_base;

########################################################################################################################
