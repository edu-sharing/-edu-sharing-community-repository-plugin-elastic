# Migrations

The Elastic track now supports different versions of the index. 
Versions are managed manually by `MigrationInfo` beans, so that only structural changes and not software version 
changes will be handled. 

Migrations are performed by a separate tracker that runs in a `migration-only` mode set via the `mode` property

Migration can be added to the `Migrations.java` by adding MigrationInfo bean
```java 
    @Bean
    @Order(0)  // ongoing order will be used to sort all MigrationInfo the latest should be the last
    public MigrationInfo migration9_0() {
        return new MigrationInfo("9.0", true);
    }
```
The migration service decides based on the current active version stored in the 'app_info' index 
and the set of required Migrations formally defined by 'MigrationInfos', wich steps are be to perform.

Migration are divided into two major steps. 
In the first step the workspace and transactions index will be copied internally by elastic search reindex api.
In the meantime the track can't add or update nodes. 

In the second step an optional reindex process, controlled by the MigrationInfo.getRequiresReindex() flag,
will reindex all nodes. So that missing entries or additional fields can be updated to the index. 
At this time the tracker will also update or add nodes. 


# Modes
Modes used to set different behaviours of the tracker like `Migration` or `FixMissing`. 
Modes can be found under org.edu_sharing.elasticsearch.elasticsearch.config.mode

# AutoConfigurationTracker
`AutoConfigurationTracker.java` contains default IndexConfiguration beans and StatusIndexService beans as well as the TransactionTracker bean.
All beans in that configuration class can be overridden e.g. by a mode configuration by defining the same bean name.  

# Index
Indices are created or deleted by the `AdminService.java`. Use the `IndexConfiguration.java` class to define new Indexes and it's settings.

# Transactions Index
Information about the tracking status are stored in the transactions index. The 'StatusIndexService.java' is a 
repository class to store and retrieve the desired information from the index.

# WorkspaceService
The `Workspace.java` formerly 'ElasticsearchClient.java' used to manage the workspace index. 

# Jobs
Tracking will be scheduled by jobs. Those are defined under `org.edu_sharing.elasticsearch.jobs` and instantiated by the `mode` configurations.

# Hints
The configuration strongly uses the spring qualifier concept. Beans of the same type will be identified by it's names.
If no bean name is be defined by the `@Bean` attribute, the method name of the Bean factory/configuration class will be used.
On injection time the bean name will be resolved by the member or attribute name if no `@Qualifier` attribute is attached to it.  

So change names of beans can result in mismatching bean configurations and should be carefully handled.    