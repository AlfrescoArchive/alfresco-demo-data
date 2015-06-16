Alfresco Demo Data
---

This module (Alfresco Repository AMP) loads some demo data into Alfresco at boot time; it can be used for demo purposes, but also for initial Repository configuration.

The project allows you to run Alfresco locally and create your content; following the steps below you will be able to simply export your data, update the project and (re)generate the AMP with the updated data that needs to be bootstrapped.

Creating content
---
First, you need to start Alfresco locally:
```
# Force using JDK8
export JAVA_HOME="`/usr/libexec/java_home -v '1.8*'`"
MAVEN_OPTS="-noverify -Xms256m -Xmx2G" mvn clean install -Ppurge,run
```

You can now access [http://localhost:8080/share](http://localhost:8080/share) and edit your contents; by default the following items are added:
- Site `demo` with 3 text files in the Project Library
- Users manager, consumer, contributor and collaborator; all user's passwords are `123`; they are all part of `demo` site, belonging to the site group reflected by the username (SiteManager, SiteConsumer, SiteContributor, SiteCollaborator)

You can change demo site data, or create a new one and set your own data structure.

Updating content
---
```
SITE_NAME=demo
curl -u admin:admin http://localhost:8080/alfresco/s/api/sites/$SITE_NAME/export > site-export.zip
unzip site-export.zip -d /tmp/site-export
mv /tmp/site-export/Contents.acp alfresco-demo-data-repo-amp/src/main/amp/config/alfresco/module/alfresco-demo-data-repo-amp/bootstrap/contents/demo.acp
rm -rf site-export.zip /tmp/site-export
```

Release
---
The release process will create the AMP artifact and upload it to Alfresco Internal Nexus; you can change `pom.xml` settings to use your own Maven Repository.

Maven is configured to only deploy the AMP artifact, saving time and bandwidth.

```
mvn release:prepare release:perform
```

Make sure that the following item is defined in your `~/.m2/settings.xml`
```
    <server>
      <id>alfresco-internal</id>
      <username>...</username>
      <password>...</password>
    </server>
```
