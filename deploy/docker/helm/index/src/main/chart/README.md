## Parameters

### Global parameters

| Name                                      | Description                                    | Value                  |
| ----------------------------------------- | ---------------------------------------------- | ---------------------- |
| `global.annotations`                      | Define global annotations                      | `{}`                   |
| `global.cluster.istio.enabled`            | Enable Istio Service mesh                      | `false`                |
| `global.cluster.pdb.enabled`              | Enable PDB                                     | `false`                |
| `global.cluster.storage.data.permission`  | Enable global custom data storage permissions  | `false`                |
| `global.cluster.storage.data.spec`        | Set data storage spec                          | `{}`                   |
| `global.cluster.storage.share.permission` | Enable global custom share storage permissions | `false`                |
| `global.cluster.storage.share.spec`       | Set share storage spec                         | `{}`                   |
| `global.cluster.sysctl.enabled`           | Enable global sysctl                           | `false`                |
| `global.debug`                            | Enable global debugging                        | `false`                |
| `global.image.pullPolicy`                 | Set global image pullPolicy                    | `Always`               |
| `global.image.pullSecrets`                | Set global image pullSecrets                   | `[]`                   |
| `global.image.registry`                   | Set global image container registry            | `${docker.registry}`   |
| `global.image.repository`                 | Set global image container repository          | `${docker.repository}` |
| `global.image.common`                     | Set global image container common              | `${docker.common}`     |
| `global.metrics.scrape.interval`          | Set prometheus scrape interval                 | `60s`                  |
| `global.metrics.scrape.timeout`           | Set prometheus scrape timeout                  | `60s`                  |
| `global.metrics.servicemonitor.enabled`   | Enable metrics service monitor                 | `false`                |
| `global.security`                         | Set global custom security parameters          | `{}`                   |

### Local parameters

| Name                                                       | Description                                                    | Value                                                                |
| ---------------------------------------------------------- | -------------------------------------------------------------- | -------------------------------------------------------------------- |
| `nameOverride`                                             | Override name                                                  | `edusharing-repository-search-elastic-index`                         |
| `image.name`                                               | Set image name                                                 | `${docker.edu_sharing.community.common.elasticsearch.name}`          |
| `image.tag`                                                | Set image tag                                                  | `${docker.edu_sharing.community.common.elasticsearch.tag}`           |
| `replicaCount`                                             | Define amount of parallel replicas to run                      | `3`                                                                  |
| `service.port.api`                                         | Set port for service API                                       | `9200`                                                               |
| `service.port.gossip`                                      | Set port for service gossip                                    | `9300`                                                               |
| `service.port.metrics`                                     | Set port for service metrics                                   | `9114`                                                               |
| `config.http.maxContentLength`                             | Set http maxContentLength                                      | `2147483647b`                                                        |
| `config.jvm.ram.minPercentage`                             | Set minimum memory in percentages                              | `75.0`                                                               |
| `config.jvm.ram.maxPercentage`                             | Set maximum memory in percentages                              | `75.0`                                                               |
| `config.store.mmap`                                        | Enable mmap store                                              | `false`                                                              |
| `multistage.enabled`                                       | Enable multistage                                              | `false`                                                              |
| `multistage.current`                                       | Set multistage current                                         | `""`                                                                 |
| `multistage.stages`                                        | Define stages for multistages                                  | `[]`                                                                 |
| `debug`                                                    | Enable debugging                                               | `false`                                                              |
| `nodeAffinity`                                             | Set node affinity                                              | `{}`                                                                 |
| `podAntiAffinity`                                          | Set pod antiaffinity                                           | `soft`                                                               |
| `tolerations`                                              | Set tolerations                                                | `[]`                                                                 |
| `persistence.data.spec.accessModes`                        | Set access modes for persistent data                           | `["ReadWriteOnce"]`                                                  |
| `persistence.data.spec.resources.requests.storage`         | Set storage request for persistent data                        | `5Gi`                                                                |
| `persistence.share.data.create`                            | Create data share                                              | `true`                                                               |
| `persistence.share.data.spec.accessModes`                  | Set access modes for persistent data share                     | `["ReadWriteMany"]`                                                  |
| `persistence.share.data.spec.resources.requests.storage`   | Set storage request for persistent data share                  | `5Gi`                                                                |
| `podAnnotations`                                           | Set custom pod annotations                                     | `{}`                                                                 |
| `podSecurityContext.fsGroup`                               | Set fs group for access                                        | `1000`                                                               |
| `podSecurityContext.fsGroupChangePolicy`                   | Set change policy for fs group                                 | `OnRootMismatch`                                                     |
| `securityContext.allowPrivilegeEscalation`                 | Allow privilege escalation                                     | `false`                                                              |
| `securityContext.capabilities.drop`                        | Set drop capabilities                                          | `["ALL"]`                                                            |
| `securityContext.runAsUser`                                | Define user to run under                                       | `1000`                                                               |
| `terminationGracePeriod`                                   | Define grace period for termination                            | `120`                                                                |
| `startupProbe.failureThreshold`                            | Failure threshold for startupProbe                             | `30`                                                                 |
| `startupProbe.initialDelaySeconds`                         | Initial delay seconds for startupProbe                         | `0`                                                                  |
| `startupProbe.periodSeconds`                               | Period seconds for startupProbe                                | `20`                                                                 |
| `startupProbe.successThreshold`                            | Success threshold for startupProbe                             | `1`                                                                  |
| `startupProbe.timeoutSeconds`                              | Timeout seconds for startupProbe                               | `10`                                                                 |
| `livenessProbe.failureThreshold`                           | Failure threshold for livenessProbe                            | `3`                                                                  |
| `livenessProbe.initialDelaySeconds`                        | Initial delay seconds for livenessProbe                        | `30`                                                                 |
| `livenessProbe.periodSeconds`                              | Period seconds for livenessProbe                               | `30`                                                                 |
| `livenessProbe.timeoutSeconds`                             | Timeout seconds for livenessProbe                              | `10`                                                                 |
| `readinessProbe.failureThreshold`                          | Failure threshold for readinessProbe                           | `1`                                                                  |
| `readinessProbe.initialDelaySeconds`                       | Initial delay seconds for readinessProbe                       | `10`                                                                 |
| `readinessProbe.periodSeconds`                             | Period seconds for readinessProbe                              | `10`                                                                 |
| `readinessProbe.successThreshold`                          | Set threshold for success on readiness probe                   | `1`                                                                  |
| `readinessProbe.timeoutSeconds`                            | Timeout seconds for readinessProbe                             | `10`                                                                 |
| `resources.limits.cpu`                                     | Set CPU limit on resources                                     | `500m`                                                               |
| `resources.limits.memory`                                  | Set memory limit on resources                                  | `2Gi`                                                                |
| `resources.requests.cpu`                                   | Set CPU for requests on resources                              | `500m`                                                               |
| `resources.requests.memory`                                | Set memory for requests on resources                           | `2Gi`                                                                |
| `init.permission.image.name`                               | Set init permission container image name                       | `${docker.edu_sharing.community.common.minideb.name}`                |
| `init.permission.image.tag`                                | Set init permission container image tag                        | `${docker.edu_sharing.community.common.minideb.tag}`                 |
| `init.permission.startupProbe`                             | Optional specs for startupProbe                                | `nil`                                                                |
| `init.permission.livenessProbe`                            | Optional specs for livenessProbe                               | `nil`                                                                |
| `init.permission.readinessProbe`                           | Optional specs for readinessProbe                              | `nil`                                                                |
| `init.permission.resources.limits.cpu`                     | Set init permission container CPU limit on resources           | `125m`                                                               |
| `init.permission.resources.limits.memory`                  | Set init permission container memory limit on resources        | `512Mi`                                                              |
| `init.permission.resources.requests.cpu`                   | Set init permission container CPU for requests on resources    | `125m`                                                               |
| `init.permission.resources.requests.memory`                | Set init permission container memory for requests on resources | `512Mi`                                                              |
| `init.permission.securityContext.runAsUser`                | Set user to run init permission container under                | `0`                                                                  |
| `init.sysctl.image.name`                                   | Set init sysctl container image name                           | `${docker.edu_sharing.community.common.minideb.name}`                |
| `init.sysctl.image.tag`                                    | Set init sysctl container image tag                            | `${docker.edu_sharing.community.common.minideb.tag}`                 |
| `init.sysctl.startupProbe`                                 | Optional specs for startupProbe                                | `nil`                                                                |
| `init.sysctl.livenessProbe`                                | Optional specs for livenessProbe                               | `nil`                                                                |
| `init.sysctl.readinessProbe`                               | Optional specs for readinessProbe                              | `nil`                                                                |
| `init.sysctl.resources.limits.cpu`                         | Set init sysctl container CPU limit on resources               | `125m`                                                               |
| `init.sysctl.resources.limits.memory`                      | Set init sysctl container memory limit on resources            | `512Mi`                                                              |
| `init.sysctl.resources.requests.cpu`                       | Set init sysctl container CPU for requests on resources        | `125m`                                                               |
| `init.sysctl.resources.requests.memory`                    | Set init sysctl container memory for requests on resources     | `512Mi`                                                              |
| `init.sysctl.securityContext.privileged`                   | Set init sysctl container to run in privileged mode            | `true`                                                               |
| `init.sysctl.securityContext.runAsUser`                    | Set user to run init sysctl container under                    | `0`                                                                  |
| `sidecar.metrics.enabled`                                  | Enable metrics sidecar container                               | `true`                                                               |
| `sidecar.metrics.image.name`                               | Set metrics sidecar image name                                 | `${docker.edu_sharing.community.common.elasticsearch.exporter.name}` |
| `sidecar.metrics.image.tag`                                | Set metrics sidecar image tag                                  | `${docker.edu_sharing.community.common.elasticsearch.exporter.tag}`  |
| `sidecar.metrics.relabelings`                              | Define relabelings for metrics sidecar container               | `[]`                                                                 |
| `sidecar.metrics.startupProbe`                             | Optional specs for startupProbe                                | `nil`                                                                |
| `sidecar.metrics.livenessProbe`                            | Optional specs for livenessProbe                               | `nil`                                                                |
| `sidecar.metrics.readinessProbe`                           | Optional specs for readinessProbe                              | `nil`                                                                |
| `sidecar.metrics.resources.limits.cpu`                     | Set metrics sidecar container CPU limit on resources           | `125m`                                                               |
| `sidecar.metrics.resources.limits.memory`                  | Set metrics sidecar container memory limit on resources        | `512Mi`                                                              |
| `sidecar.metrics.resources.requests.cpu`                   | Set metrics sidecar container CPU for requests on resources    | `125m`                                                               |
| `sidecar.metrics.resources.requests.memory`                | Set metrics sidecar container memory for requests on resources | `512Mi`                                                              |
| `sidecar.metrics.securityContext.allowPrivilegeEscalation` | Allow privilege escalation for metrics sidecar                 | `false`                                                              |
| `sidecar.metrics.securityContext.capabilities.drop`        | Set drop capabilities for metrics sidecar                      | `["ALL"]`                                                            |
| `sidecar.metrics.securityContext.runAsUser`                | Define user to run metrics sidecar under                       | `1000`                                                               |
