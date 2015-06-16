Alfresco Demo Data
---

This module loads some demo data into Alfresco at boot time; it also provides a way to create and export demo data (using the [share-import-export add-on](https://addons.alfresco.com/addons/importexport-acpzip-share))

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

Download site ACP
---
```
curl -u admin:admin http://localhost:8080/alfresco/s/api/sites/demo-site/export > demo-site.zip
```
Where `demo-site` is the name of the site we want to export
