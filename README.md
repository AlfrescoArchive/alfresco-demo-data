Alfresco Demo Data
---

This module loads some demo data into Alfresco at boot time; it also provides a Share local interface to load data; you can export data using `curl/wget` (see below)

Test local
---

```
# Force using JDK8
export JAVA_HOME="`/usr/libexec/java_home -v '1.8*'`"
MAVEN_OPTS="-noverify -Xms256m -Xmx2G" mvn clean install -Ppurge,run
```

Deploy/Release
---
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

Download site ACP
---
```
curl -u admin:admin http://localhost:8080/alfresco/s/api/sites/demo-site/export > demo-site.zip
```
Where `demo-site` is the name of the site we want to export
