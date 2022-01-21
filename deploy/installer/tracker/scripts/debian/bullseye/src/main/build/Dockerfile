########################################################################################################################
#                                                                                                                      #
#         !!!         THIS DOCKER IS FOR TESTING ONLY PURPOSES AND NOT FOR PRODUCTIVE DEPLOYMENT         !!!           #
#                                                                                                                      #
########################################################################################################################

FROM docker.io/${os.distribution}:${os.codename}

ENV EDU_HOME /opt/edu-sharing
#ENV PATH $ALF_HOME/bin:$PATH
WORKDIR $EDU_HOME

RUN set -eux \
    && ln -sf /usr/share/zoneinfo/Europe/Berlin /etc/localtime

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        locales \
    && sed -i 's/^\(\\s+\)/# \1/' /etc/locale.gen \
    && sed -i 's/^# *\(de_DE.UTF-8\)/\1/' /etc/locale.gen \
    && locale-gen \
    && update-locale LANG=de_DE.utf8 \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        apt-transport-https \
        ca-certificates \
        gnupg \
        lsb-release \
        procps \
        unzip \
        wget \
        ruby-hocon \
        xmlstarlet \
        systemctl \
        wait-for-it \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*


RUN set -eux \
    && wget -q https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public \
    && gpg --no-default-keyring --keyring ./adoptopenjdk-keyring.gpg --import public \
    && gpg --no-default-keyring --keyring ./adoptopenjdk-keyring.gpg --export --output adoptopenjdk-archive-keyring.gpg \
    && mv adoptopenjdk-archive-keyring.gpg /usr/share/keyrings \
    && rm adoptopenjdk-keyring.gpg public \
    && echo "deb [signed-by=/usr/share/keyrings/adoptopenjdk-archive-keyring.gpg] https://adoptopenjdk.jfrog.io/adoptopenjdk/deb `lsb_release -cs` main" | tee /etc/apt/sources.list.d/adoptopenjdk.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        adoptopenjdk-8-hotspot \
    && update-java-alternatives -s adoptopenjdk-8-hotspot-amd64 \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*


RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
	    maven=3.6.3-* \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

RUN set -eux \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        curl \
        dnsutils \
        jq \
        lsof \
        nano \
        net-tools \
        vim \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*


########################################################################################################################

RUN set -eux \
    && wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | apt-key add - \
    && echo "deb https://artifacts.elastic.co/packages/7.x/apt stable main" | tee /etc/apt/sources.list.d/elastic-7.x.list \
    && apt-get update -y \
    && apt-get install -y --no-install-recommends \
        elasticsearch \
    && mkdir /var/run/elasticsearch \
    && chown elasticsearch:elasticsearch -R /var/run/elasticsearch \
    && chown elasticsearch:elasticsearch -R /usr/share/elasticsearch \
    && chown elasticsearch:elasticsearch -R /var/log/elasticsearch \
    && chown elasticsearch:elasticsearch -R /var/lib/elasticsearch \
    && chown elasticsearch:elasticsearch -R /etc/default/elasticsearch \
    && chown elasticsearch:elasticsearch -R /etc/elasticsearch \
    && apt-get clean \
    && rm -r /var/lib/apt/lists/*

COPY --chown=elasticsearch:elasticsearch assets/config/elasticsearch.yml /etc/elasticsearch/elasticsearch.yml

########################################################################################################################

COPY install.sh .env.base ./
COPY assets/entrypoint.sh ./
COPY assets/settings.xml /root/.m2/

RUN set -eux \
    && chmod +x install.sh \
    && chmod +x entrypoint.sh

########################################################################################################################

ENTRYPOINT ["./entrypoint.sh"]

EXPOSE 8080 8081 9200

#VOLUME alf_data

LABEL git.branch=${git.branch}
LABEL git.closest.tag.name=${git.closest.tag.fixed}
LABEL git.commit.id=${git.commit.id}
LABEL git.dirty=${git.dirty}
LABEL mvn.project.artifactId=${project.artifactId}
LABEL mvn.project.groupId=${project.groupId}
LABEL mvn.project.version=${project.version}
